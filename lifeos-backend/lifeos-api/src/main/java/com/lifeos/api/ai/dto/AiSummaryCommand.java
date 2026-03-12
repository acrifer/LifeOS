package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiSummaryCommand implements Serializable {
    private Long noteId;
    private Long userId;
    private String title;
    private String content;
}
