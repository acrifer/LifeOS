package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DashboardStatsDTO implements Serializable {
    private Long pendingTaskCount;
    private Long noteCount;
    private Long notesToReviewCount;
    private Long weekNewNoteCount;
    private Long weekOrganizedNoteCount;
    private Long weekCompletedTaskCount;
    private Long pendingExtractedTaskCount;
    private Long aiInboxCount;
    private List<DashboardTagStatDTO> topTags;
    private List<DashboardRecentNoteDTO> recentNotes;
    private List<BehaviorTrendItemDTO> recentTrend;
}
