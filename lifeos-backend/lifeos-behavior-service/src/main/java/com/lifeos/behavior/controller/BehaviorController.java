package com.lifeos.behavior.controller;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.behavior.service.BehaviorService;
import com.lifeos.common.response.Result;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/behavior")
@Tag(name = "行为分析", description = "知识仪表盘和行为统计接口")
public class BehaviorController {

    @Resource
    private BehaviorService behaviorService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取知识仪表盘", description = "返回当前用户的复习、AI、任务和主题统计数据。")
    public Result<DashboardStatsDTO> getDashboard(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(behaviorService.getDashboardStats(userId));
    }

    @PostMapping("/internal/event")
    @Hidden
    public Result<Void> recordEvent(@RequestBody BehaviorEventCommand command) {
        behaviorService.recordEvent(command);
        return Result.success();
    }

    @GetMapping("/internal/dashboard")
    @Hidden
    public Result<DashboardStatsDTO> getDashboardInternal(@RequestParam("userId") Long userId) {
        return Result.success(behaviorService.getDashboardStats(userId));
    }
}
