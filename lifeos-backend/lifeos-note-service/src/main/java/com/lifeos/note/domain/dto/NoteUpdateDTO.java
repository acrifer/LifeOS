package com.lifeos.note.domain.dto;

import lombok.Data;

@Data
public class NoteUpdateDTO {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String summary; // Used when AI returns summary
}
