package com.lifeos.note.controller;

import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.common.response.Result;
import com.lifeos.note.service.AiJobService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/note")
@Slf4j
@Tag(name = "AI 作业", description = "异步 AI 任务查询、周复盘和状态回写")
public class AiJobController {

    @Resource
    private AiJobService aiJobService;

    @GetMapping("/jobs")
    @Operation(summary = "查询 AI 作业列表", description = "按笔记或任务类型筛选当前用户的异步 AI 作业。")
    public Result<List<AiAsyncJobDTO>> listJobs(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "noteId", required = false) Long noteId,
            @RequestParam(name = "jobType", required = false) String jobType,
            @RequestParam(name = "limit", required = false) Integer limit) {
        try {
            return Result.success(aiJobService.listJobs(userId, noteId, jobType, limit));
        } catch (Exception ex) {
            log.error("Failed to list ai jobs", ex);
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "查询 AI 作业详情", description = "返回指定 AI 作业的状态、结果和错误信息。")
    public Result<AiAsyncJobDTO> getJob(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("jobId") Long jobId) {
        try {
            return Result.success(aiJobService.getJob(userId, jobId));
        } catch (Exception ex) {
            log.error("Failed to get ai job {}", jobId, ex);
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/weekly-review")
    @Operation(summary = "创建周复盘作业", description = "提交异步周复盘任务，后续通过作业接口轮询结果。")
    public Result<AiAsyncJobDTO> createWeeklyReview(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            return Result.success(aiJobService.submitWeeklyReview(userId));
        } catch (Exception ex) {
            log.error("Failed to create weekly review job", ex);
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/internal/jobs/{jobId}/status")
    @Hidden
    public Result<Void> updateJobStatus(@PathVariable("jobId") Long jobId,
            @RequestBody AiAsyncJobUpdateDTO request) {
        try {
            aiJobService.updateJobStatus(jobId, request);
            return Result.success();
        } catch (Exception ex) {
            log.error("Failed to update ai job {}", jobId, ex);
            return Result.error(ex.getMessage());
        }
    }
}
