package com.drdoc.service;

import com.drdoc.dto.AskResponseDto;
import com.drdoc.dto.SourceDto;

import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.model.chat.ChatModel;

import dev.langchain4j.store.embedding.EmbeddingMatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAnswerService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ChatModel chatModel;
    private final BM25Tokenizer bm25Tokenizer;

    // =========================
    // Ask Question
    // =========================

    public AskResponseDto askQuestion(String question, String documentId) {

        try {
            log.info("Question received: {}", question);
            // =========================
            // Create Query Embedding
            // =========================
            String searchQuery = question
                    .toLowerCase()
                    .replace("what is ", "")
                    .replace("what are ", "")
                    .replace("explain ", "")
                    .replace("tell me about ", "")
                    .trim();

            float[] queryEmbedding = embeddingService.createEmbedding(searchQuery);

            if (queryEmbedding == null) {
                return AskResponseDto.builder()
                        .answer("Failed to generate question embedding.")
                        .sources(List.of())
                        .build();
            }

            // =========================
            // Retrieve Chunks
            // =========================

            Map<Integer, Float> sparseWeights = bm25Tokenizer.computeSparseVector(searchQuery);
            List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.hybridSearch(
                    queryEmbedding, sparseWeights, 8, documentId);
            if (matches.isEmpty()) {
                return AskResponseDto.builder()
                        .answer("No relevant information found.")
                        .sources(List.of())
                        .build();
            }

            // =========================
            // Build Context
            // =========================

            StringBuilder contextBuilder = new StringBuilder();
            List<SourceDto> sources = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                String text = segment.text();
                contextBuilder.append(text).append("\n\n");
                Integer page = null;
                try {
                    page = segment.metadata().getInteger("page");
                } catch (Exception ignored) {
                }
                sources.add(SourceDto.builder().page(page).snippet(text.substring(0, Math.min(200, text.length()))).build());
            }

            String context = contextBuilder.toString();

            // =========================
            // Build Prompt
            // =========================
            log.info("Context being sent to LLM: {}", context);
            String prompt = """
                    You are a helpful assistant that answers questions based on the provided document context.
                    
                    Use the context below to answer the question as completely as possible.
                    If the answer is partially available, provide what you can find.
                    Only if the topic is completely absent from the context, say:
                    "This information is not available in the document."
                    
                    Context:
                    %s
                    
                    Question: %s
                    
                    Answer:
                    """.formatted(context, question);

            // =========================
            // Generate Answer
            // =========================

            String answer = chatModel.chat(prompt);
            return AskResponseDto.builder().answer(answer).sources(sources).build();
        } catch (Exception e) {
            log.error("Error answering question", e);
            throw new RuntimeException("Question answering failed");
        }
    }
}