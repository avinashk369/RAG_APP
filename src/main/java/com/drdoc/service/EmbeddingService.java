package com.drdoc.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    // =========================
    // Create Embedding
    // =========================

    public float[] createEmbedding(String text) {

        try {
            text = text.trim();
            if (text.isEmpty()) {
                log.warn("Empty text for embedding");
                return null;
            }

            // Safety limit
            if (text.length() > 8000) {
                log.warn("Chunk too large, skipping embedding");
                return null;
            }

            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vector();
            // IMPORTANT VALIDATION
            if (vector == null || vector.length == 0) {
                log.error("Embedding model returned empty vector");
                return null;
            }

            log.info("Embedding created successfully. Dimension: {}", vector.length);
            return vector;
        } catch (Exception e) {
            log.error("Error creating embedding", e);
            return null;
        }
    }
}