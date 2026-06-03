package com.drdoc.config;

import io.qdrant.client.QdrantClient;

import io.qdrant.client.grpc.Collections;

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
            log.info("INITIALIZER STARTED");
            List<String> collections = qdrantClient.listCollectionsAsync().get();
            boolean exists = collections.contains(collectionName);

            if (!exists) {
                qdrantClient.createCollectionAsync(collectionName, Collections.VectorParams.newBuilder().setSize(768).setDistance(Collections.Distance.Cosine).build()).get();
                log.info("Collection created: {}", collectionName);
            } else {
                log.info("Collection already exists: {}", collectionName);
            }

        } catch (Exception e) {
            log.error("COLLECTION CREATION FAILED", e);
        }
    }
}