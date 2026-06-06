package com.drdoc.service;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Points.Fusion;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PrefetchQuery;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.fusion;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorFactory.vector;
import static io.qdrant.client.VectorsFactory.namedVectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;



@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final QdrantClient qdrantClient;
    private final BM25Tokenizer bm25Tokenizer;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    // =========================
    // Store Embedding (Dense + Sparse)
    // =========================
    public void storeEmbedding(String text, float[] denseVector, String documentId,
                               String documentName, Integer page) {
        try {
            // Compute sparse BM25 vector
            Map<Integer, Float> sparseWeights = bm25Tokenizer.computeSparseVector(text);
            List<Integer> sparseIndices = new ArrayList<>(sparseWeights.keySet());
            List<Float> sparseValues = sparseIndices.stream()
                    .map(sparseWeights::get)
                    .toList();

            // Dense vector as List<Float>
            List<Float> denseList = new ArrayList<>();
            for (float f : denseVector) denseList.add(f);

            // Build payload
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
            payload.put("text",          value(text));
            payload.put("document_id",   value(documentId));
            payload.put("document_name", value(documentName));
            payload.put("page",          value(page));

            // Named vectors: dense (List<Float>) + sparse (indices + values)
            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.randomUUID()))
                    .setVectors(namedVectors(Map.of(
                            "dense",  vector(denseList),
                            "sparse", vector(sparseValues, sparseIndices)  // sparse overload
                    )))
                    .putAllPayload(payload)
                    .build();

            qdrantClient.upsertAsync(collectionName, List.of(point)).get();
            log.info("Stored dense+sparse embedding for page {}", page);

        } catch (Exception e) {
            log.error("Error storing embedding", e);
            throw new RuntimeException("Failed to store embedding", e);
        }
    }

    // =========================
    // Hybrid Search (Dense + Sparse via RRF)
    // =========================
    public List<EmbeddingMatch<TextSegment>> hybridSearch(float[] denseVector,
                                                          Map<Integer, Float> sparseWeights,
                                                          int limit, String documentId) {
        try {
            // Dense query
            List<Float> denseList = new ArrayList<>();
            for (float f : denseVector) denseList.add(f);

            // Sparse query
            List<Integer> sparseIndices = new ArrayList<>(sparseWeights.keySet());
            List<Float> sparseValues = sparseIndices.stream()
                    .map(sparseWeights::get)
                    .toList();

            // Document filter
            Filter filter = Filter.newBuilder()
                    .addMust(matchKeyword("document_id", documentId))
                    .build();


            // Dense prefetch
            PrefetchQuery densePrefetch = PrefetchQuery.newBuilder()
                    .setQuery(nearest(denseList))           // dense: List<Float>
                    .setUsing("dense")
                    .setFilter(filter)
                    .setLimit(limit * 2L)
                    .build();

            // Sparse prefetch
            PrefetchQuery sparsePrefetch = PrefetchQuery.newBuilder()
                    .setQuery(nearest(sparseValues, sparseIndices))  // sparse: values + indices
                    .setUsing("sparse")
                    .setFilter(filter)
                    .setLimit(limit * 2L)
                    .build();

            // RRF fusion query
            QueryPoints queryPoints = QueryPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addPrefetch(densePrefetch)
                    .addPrefetch(sparsePrefetch)
                    .setQuery(fusion(Fusion.RRF))            // RRF merge
                    .setLimit(limit)
                    .setWithPayload(enable(true))
                    .build();

            List<ScoredPoint> points = qdrantClient.queryAsync(queryPoints).get();
            log.info("Hybrid search returned {} matches for documentId: {}", points.size(), documentId);

            return points.stream().map(this::toEmbeddingMatch).toList();

        } catch (Exception e) {
            log.error("Error in hybrid search", e);
            return List.of();
        }
    }

    // =========================
    // Convert ScoredPoint -> EmbeddingMatch
    // =========================
    private EmbeddingMatch<TextSegment> toEmbeddingMatch(ScoredPoint point) {

        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();

        String text    = payload.get("text").getStringValue();
        int page       = (int) payload.get("page").getIntegerValue();
        String docId   = payload.get("document_id").getStringValue();
        String docName = payload.get("document_name").getStringValue();

        Metadata metadata = new Metadata();
        metadata.put("document_id",   docId);
        metadata.put("document_name", docName);
        metadata.put("page",          page);

        TextSegment segment = TextSegment.from(text, metadata);

        return new EmbeddingMatch<>(
                (double) point.getScore(),
                point.getId().getUuid(),
                Embedding.from(new float[0]),
                segment
        );
    }
}