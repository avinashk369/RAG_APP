package com.drdoc.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.data.embedding.Embedding;

import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

import dev.langchain4j.store.embedding.EmbeddingStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;


@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore;

    // =========================
    // Store Embedding
    // =========================

    public void storeEmbedding(String text, float[] vector, String documentId, String documentName, Integer page) {
        try {
            Metadata metadata = new Metadata();
            metadata.put("document_id", documentId);
            metadata.put("document_name", documentName);
            metadata.put("page", page);

            TextSegment segment = TextSegment.from(text, metadata);
            Embedding embedding = Embedding.from(vector);
            embeddingStore.add(embedding, segment);
            log.info("Stored embedding for page {}", page);
        } catch (Exception e) {
            log.error("Error storing embedding", e);
            throw new RuntimeException("Failed to store embedding", e);
        }
    }

    // =========================
    // Search Embeddings
    // =========================

    public List<EmbeddingMatch<TextSegment>> search(float[] vector, int limit, String documentId) {

        try {
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(vector))
                    .maxResults(limit)
                    .minScore(0.2)
                    .filter(metadataKey("document_id").isEqualTo(documentId)) // add filter
                    .build();

            log.info("Searching with documentId filter: {}", documentId);
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            log.info("Search returned {} matches", result.matches().size());
            return result.matches();

        } catch (Exception e) {
            log.error("Error searching embeddings", e);
            return List.of();
        }
    }
}