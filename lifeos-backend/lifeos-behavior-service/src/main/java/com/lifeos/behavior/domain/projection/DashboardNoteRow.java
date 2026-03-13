package com.lifeos.behavior.domain.projection;

import lombok.Data;

import java.util.Date;

@Data
public class DashboardNoteRow {
    private Long id;
    private String title;
    private String tags;
    private String summary;
    private String reviewState;
    private Boolean pinned;
    private Date updateTime;
}
