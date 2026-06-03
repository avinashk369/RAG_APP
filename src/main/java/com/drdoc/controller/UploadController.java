package com.drdoc.controller;

import com.drdoc.dto.UploadResponseDto;
import com.drdoc.service.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UploadController {

    private final DocumentIngestionService ingestionService;

    // =========================
    // Upload PDF
    // =========================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponseDto uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {

        // =========================
        // Generate Document ID
        // =========================

        UUID documentId = UUID.randomUUID();

        // =========================
        // Convert MultipartFile -> File
        // =========================

        File tempFile = File.createTempFile("upload-", ".pdf");
        try {
            file.transferTo(tempFile);
            Integer chunksCreated = ingestionService.ingestDocument(tempFile, documentId, file.getOriginalFilename());
            return UploadResponseDto.builder()
                    .message("PDF processed successfully")
                    .documentId(documentId.toString())
                    .documentName(file.getOriginalFilename())
                    .chunksCreated(chunksCreated)
                    .build();
        } finally {
            tempFile.delete();
        }
    }
}