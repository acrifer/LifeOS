package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiTaskExtractionResult implements Serializable {
    private Long noteId;
    private List<AiTaskSuggestionDTO> tasks = new ArrayList<>();
    private String status;
    private String errorMessage;
}
