package com.lifeos.note.domain.dto;

import lombok.Data;

@Data
public class NoteCreateDTO {
    private String title;
    private String content;
    private String tags;
    private Boolean pinned;
    private String reviewState;
    private java.util.Date nextReviewAt;
}
