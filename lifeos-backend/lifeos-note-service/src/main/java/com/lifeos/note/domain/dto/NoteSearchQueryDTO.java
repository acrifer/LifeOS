package com.lifeos.note.domain.dto;

import lombok.Data;

@Data
public class NoteSearchQueryDTO {
    private String keyword;
    private String tags;
    private Boolean pinned;
    private String reviewState;
    private Boolean hasSummary;
    private Boolean needsOrganization;
    private String sort;
}
