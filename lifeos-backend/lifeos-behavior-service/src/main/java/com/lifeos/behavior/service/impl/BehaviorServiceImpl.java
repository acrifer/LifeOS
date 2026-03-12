package com.lifeos.behavior.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.BehaviorTrendItemDTO;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.behavior.domain.entity.UserBehavior;
import com.lifeos.behavior.domain.projection.BehaviorTrendRow;
import com.lifeos.behavior.mapper.BehaviorMapper;
import com.lifeos.behavior.service.BehaviorService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BehaviorServiceImpl extends ServiceImpl<BehaviorMapper, UserBehavior> implements BehaviorService {

    private static final String FINISH_TASK = "FINISH_TASK";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public void recordEvent(BehaviorEventCommand command) {
        if (command == null || command.getUserId() == null || !StringUtils.hasText(command.getActionType())) {
            throw new RuntimeException("Invalid behavior event");
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(command.getUserId());
        behavior.setActionType(command.getActionType().trim().toUpperCase(Locale.ROOT));
        behavior.setTargetId(command.getTargetId());
        this.save(behavior);
    }

    @Override
    public DashboardStatsDTO getDashboardStats(Long userId) {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setPendingTaskCount(defaultZero(baseMapper.countPendingTasks(userId)));
        stats.setNoteCount(defaultZero(baseMapper.countNotes(userId)));
        stats.setWeekCompletedTaskCount(defaultZero(baseMapper.countEventsSince(userId, FINISH_TASK, LocalDateTime.now().minusDays(6))));
        stats.setRecentTrend(buildRecentTrend(userId));
        return stats;
    }

    private List<BehaviorTrendItemDTO> buildRecentTrend(Long userId) {
        LocalDate startDate = LocalDate.now().minusDays(6);
        Map<LocalDate, Long> trendMap = baseMapper.selectRecentTrend(userId, startDate.atStartOfDay()).stream()
                .collect(Collectors.toMap(BehaviorTrendRow::getEventDate, BehaviorTrendRow::getActionCount));

        List<BehaviorTrendItemDTO> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            BehaviorTrendItemDTO item = new BehaviorTrendItemDTO();
            item.setDate(currentDate.format(DATE_FORMATTER));
            item.setCount(trendMap.getOrDefault(currentDate, 0L));
            trend.add(item);
        }
        return trend;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
