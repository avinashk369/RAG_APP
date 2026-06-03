package com.drdoc.controller;

import com.drdoc.dto.AskRequestDto;
import com.drdoc.dto.AskResponseDto;

import com.drdoc.service.QuestionAnswerService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AskController {

    private final QuestionAnswerService questionAnswerService;

    // =========================
    // Ask Question
    // =========================

    @PostMapping("/ask")
    public AskResponseDto askQuestion(@RequestBody AskRequestDto request) {
        return questionAnswerService.askQuestion(request.getQuestion(),request.getDocumentId());
    }
}