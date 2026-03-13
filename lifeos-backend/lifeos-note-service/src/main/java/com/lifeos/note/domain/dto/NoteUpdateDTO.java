package com.lifeos.note.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class NoteUpdateDTO {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String summary; // Used when AI returns summary
    private Boolean pinned;
    private String reviewState;
    private Date nextReviewAt;
    private Date lastReviewedAt;
}
