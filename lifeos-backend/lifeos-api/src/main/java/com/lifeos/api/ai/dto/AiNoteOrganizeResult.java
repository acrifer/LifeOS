package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiNoteOrganizeResult implements Serializable {
    private Long noteId;
    private String suggestedTitle;
    private String suggestedTags;
    private String summary;
    private String status;
    private String errorMessage;
}
