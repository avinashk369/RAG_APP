package com.drdoc.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BM25Tokenizer {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    // =========================
    // Compute sparse vector from text
    // Returns map of termId -> weight
    // =========================
    public Map<Integer, Float> computeSparseVector(String text) {

        List<String> tokens = tokenize(text);

        if (tokens.isEmpty()) return Map.of();

        // Term frequency
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }

        // BM25 weight per term (simplified — no corpus IDF, suitable for single-doc scoring)
        Map<Integer, Float> sparse = new HashMap<>();
        int docLength = tokens.size();

        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            String term = entry.getKey();
            int freq = entry.getValue();

            // TF normalization with BM25 formula
            double tfNorm = (freq * (K1 + 1.0))
                    / (freq + K1 * (1.0 - B + B * docLength / 10.0));

            int termId = Math.abs(term.hashCode());  // term -> int id
            sparse.put(termId, (float) tfNorm);
        }

        return sparse;
    }

    // =========================
    // Tokenize: lowercase, split, remove stopwords
    // =========================
    private List<String> tokenize(String text) {

        Set<String> stopwords = Set.of(
                "the", "a", "an", "is", "it", "in", "on", "at", "to",
                "for", "of", "and", "or", "be", "are", "was", "were",
                "this", "that", "with", "as", "by", "from", "has", "have"
        );

        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");

        List<String> tokens = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 2 && !stopwords.contains(word)) {
                tokens.add(word);
            }
        }

        return tokens;
    }
}