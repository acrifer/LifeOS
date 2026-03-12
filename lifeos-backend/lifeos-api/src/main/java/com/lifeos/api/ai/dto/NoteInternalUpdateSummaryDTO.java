package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class NoteInternalUpdateSummaryDTO implements Serializable {
    private Long userId;
    private String summary;
}
