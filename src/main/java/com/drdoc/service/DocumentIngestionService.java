package com.drdoc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.File;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final PdfService pdfService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    // =========================
    // Ingest Document
    // =========================

    public Integer ingestDocument(File file, UUID documentId, String documentName) {
        try {
            log.info("Starting document ingestion: {}", documentName);
            // =========================
            // Extract + Chunk by Page
            // =========================
            Map<Integer, List<String>> pageChunks = pdfService.extractAndChunkByPage(file, 800, 150);
            int storedChunks = 0;

            for (Map.Entry<Integer, List<String>> entry : pageChunks.entrySet()) {
                int pageNumber = entry.getKey();  // real page number now
                for (String chunk : entry.getValue()) {
                    chunk = chunk.trim();
                    if (chunk.isEmpty()) continue;

                    float[] embedding = embeddingService.createEmbedding(chunk);

                    if (embedding == null || embedding.length == 0) {
                        log.warn("Invalid embedding. Skipping chunk on page {}", pageNumber);
                        continue;
                    }

                    if (embedding.length != 768) {
                        log.warn("Invalid embedding dimension: {} on page {}", embedding.length, pageNumber);
                        continue;
                    }
                    vectorStoreService.storeEmbedding(chunk, embedding, documentId.toString(), documentName, pageNumber);
                    storedChunks++;
                }
            }
            log.info("Document ingestion completed. Total chunks stored: {}", storedChunks);
            return storedChunks;
        } catch (Exception e) {
            log.error("Error ingesting document", e);
            throw new RuntimeException("Document ingestion failed");
        }
    }
}