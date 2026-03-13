package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardRecentNoteDTO implements Serializable {
    private String id;
    private String title;
    private String tags;
    private String reviewState;
    private boolean pinned;
    private boolean hasSummary;
    private String updatedAt;
}
