package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiSummaryResult implements Serializable {
    private Long noteId;
    private String summary;
    private String status;
    private String errorMessage;
}
