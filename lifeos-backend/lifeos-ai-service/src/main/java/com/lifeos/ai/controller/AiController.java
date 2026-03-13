package com.lifeos.ai.controller;

import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiNoteOrganizeResult;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.AiTaskExtractionResult;
import com.lifeos.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI 能力", description = "摘要、整理和任务提取等 AI 处理能力")
public class AiController {

    @Resource
    private AiSummaryService aiSummaryService;

    @PostMapping("/note-summary")
    @Operation(summary = "生成笔记摘要", description = "根据标题、标签和正文内容生成精简摘要。")
    public Result<AiSummaryResult> generateNoteSummary(@RequestBody AiSummaryCommand command) {
        return Result.success(aiSummaryService.generateSummary(command));
    }

    @PostMapping("/note-organize")
    @Operation(summary = "整理笔记", description = "生成推荐标题、标签和摘要建议。")
    public Result<AiNoteOrganizeResult> organizeNote(@RequestBody AiSummaryCommand command) {
        return Result.success(aiSummaryService.organizeNote(command));
    }

    @PostMapping("/note-task-extract")
    @Operation(summary = "提取行动项", description = "从笔记内容中提取可以执行的任务建议。")
    public Result<AiTaskExtractionResult> extractTasks(@RequestBody AiSummaryCommand command) {
        return Result.success(aiSummaryService.extractTasks(command));
    }
}
