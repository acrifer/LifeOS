package com.lifeos.behavior.controller;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.behavior.service.BehaviorService;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/behavior")
public class BehaviorController {

    @Resource
    private BehaviorService behaviorService;

    @GetMapping("/dashboard")
    public Result<DashboardStatsDTO> getDashboard(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(behaviorService.getDashboardStats(userId));
    }

    @PostMapping("/internal/event")
    public Result<Void> recordEvent(@RequestBody BehaviorEventCommand command) {
        behaviorService.recordEvent(command);
        return Result.success();
    }
}
