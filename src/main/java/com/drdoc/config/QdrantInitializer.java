package com.drdoc.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration("qdrantInitializer")
@RequiredArgsConstructor
public class QdrantInitializer {

    private final QdrantClient qdrantClient;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    @PostConstruct
    public void initializeCollection() {
        try {
            log.info("INITIALIZER STARTED - connecting to Qdrant...");

            List<String> collections = qdrantClient.listCollectionsAsync().get();

            if (collections.contains(collectionName)) {
                log.info("Collection already exists: {}", collectionName);
                return;
            }

            // Dense + Sparse named vector config
            CreateCollection request = CreateCollection.newBuilder()
                    .setCollectionName(collectionName)
                    .setVectorsConfig(
                            VectorsConfig.newBuilder()
                                    .setParamsMap(
                                            VectorParamsMap.newBuilder()
                                                    .putMap("dense", VectorParams.newBuilder()
                                                            .setSize(768)
                                                            .setDistance(Distance.Cosine)
                                                            .build())
                                                    .build())
                                    .build())
                    .setSparseVectorsConfig(
                            SparseVectorConfig.newBuilder()
                                    .putMap("sparse", SparseVectorParams.newBuilder()
                                            .build())
                                    .build())
                    .build();

            qdrantClient.createCollectionAsync(request).get();
            log.info("Collection created with dense + sparse vectors: {}", collectionName);

        } catch (Exception e) {
            log.error("FAILED TO CONNECT TO QDRANT — is it running? Check: docker compose up -d", e);
            throw new RuntimeException("Qdrant is not available", e);
        }
    }
}