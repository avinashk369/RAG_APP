package com.drdoc.dto;

import lombok.Data;

@Data
public class AskRequestDto {
    private String question;
    private String documentId;
}