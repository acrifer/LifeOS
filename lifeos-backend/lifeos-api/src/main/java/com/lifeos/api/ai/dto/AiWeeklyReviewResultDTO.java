package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiWeeklyReviewResultDTO implements Serializable {
    private String headline;
    private String summary;
    private List<String> highlights = new ArrayList<>();
    private List<String> focusAreas = new ArrayList<>();
    private List<String> nextActions = new ArrayList<>();
}
