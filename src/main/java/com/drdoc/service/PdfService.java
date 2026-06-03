package com.drdoc.service;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PdfService {

    // ADD this new method alongside extractText()
    public Map<Integer, List<String>> extractAndChunkByPage(File file, int chunkSize, int overlap) throws IOException {

        Map<Integer, List<String>> pageChunks = new LinkedHashMap<>();

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document).replaceAll("\\s+", " ").trim();

                log.info("Page {} raw text: {}", i, pageText);
                if (pageText.isEmpty()) continue;
                List<String> chunks = chunkText(pageText, chunkSize, overlap);
                pageChunks.put(i, chunks);
            }
        }
        log.info("Extracted text from {} pages", pageChunks.size());
        return pageChunks;
    }




    // =========================
    // Chunk Text
    // =========================

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        text = text.replaceAll("\\s+", " ").trim();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // Snap to word boundary — don't cut mid-word
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start += (chunkSize - overlap);
        }
        log.info("Total chunks created: {}", chunks.size());
        return chunks;
    }


}