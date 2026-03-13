package com.lifeos.note.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class NoteReviewDTO {
    private String reviewState;
    private Date nextReviewAt;
}
