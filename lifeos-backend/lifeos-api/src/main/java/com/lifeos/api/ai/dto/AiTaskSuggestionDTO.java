package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiTaskSuggestionDTO implements Serializable {
    private String title;
    private String description;
    private String tags;
}
