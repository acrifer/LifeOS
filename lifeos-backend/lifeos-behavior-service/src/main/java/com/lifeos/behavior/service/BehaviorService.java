package com.lifeos.behavior.service;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;

public interface BehaviorService {
    void recordEvent(BehaviorEventCommand command);

    DashboardStatsDTO getDashboardStats(Long userId);
}
