package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DashboardStatsDTO implements Serializable {
    private Long pendingTaskCount;
    private Long noteCount;
    private Long weekCompletedTaskCount;
    private List<BehaviorTrendItemDTO> recentTrend;
}
