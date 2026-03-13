package com.lifeos.note.controller;

import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.NoteInternalUpdateSummaryDTO;
import com.lifeos.common.response.Result;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NotePinDTO;
import com.lifeos.note.domain.dto.NoteReviewDTO;
import com.lifeos.note.domain.dto.NoteSearchQueryDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.service.NoteService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/note")
@Tag(name = "知识笔记", description = "知识记录、搜索、复习和 AI 工作流入口")
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping
    @Operation(summary = "创建笔记", description = "创建新的知识笔记，并初始化复习状态。")
    public Result<String> createNote(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody NoteCreateDTO createDTO) {
        try {
            Long noteId = noteService.createNote(userId, createDTO);
            return Result.success(String.valueOf(noteId));
        } catch (Exception e) {
            log.error("Failed to create note", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "更新笔记", description = "更新标题、正文、标签和摘要等基础信息。")
    public Result<Void> updateNote(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody NoteUpdateDTO updateDTO) {
        try {
            noteService.updateNote(userId, updateDTO);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "删除当前用户拥有的指定笔记。")
    public Result<Void> deleteNote(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            noteService.deleteNote(userId, noteId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记详情", description = "返回指定笔记的完整内容和知识属性。")
    public Result<Note> getNoteDetail(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            Note note = noteService.getNoteDetail(userId, noteId);
            return Result.success(note);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取笔记列表", description = "按更新时间返回当前用户的全部笔记。")
    public Result<List<Note>> listUserNotes(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            List<Note> notes = noteService.listUserNotes(userId);
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索笔记", description = "支持关键词、标签、复习状态、摘要状态和排序筛选。")
    public Result<List<Note>> searchNotes(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestParam(name = "pinned", required = false) Boolean pinned,
            @RequestParam(name = "reviewState", required = false) String reviewState,
            @RequestParam(name = "hasSummary", required = false) Boolean hasSummary,
            @RequestParam(name = "needsOrganization", required = false) Boolean needsOrganization,
            @RequestParam(name = "sort", required = false) String sort) {
        try {
            NoteSearchQueryDTO query = new NoteSearchQueryDTO();
            query.setKeyword(keyword);
            query.setTags(tags);
            query.setPinned(pinned);
            query.setReviewState(reviewState);
            query.setHasSummary(hasSummary);
            query.setNeedsOrganization(needsOrganization);
            query.setSort(sort);
            List<Note> notes = noteService.searchNotes(userId, query);
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/pin")
    @Operation(summary = "更新置顶状态", description = "设置或取消当前笔记的置顶状态。")
    public Result<Void> updatePin(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId,
            @RequestBody NotePinDTO request) {
        try {
            noteService.updatePin(userId, noteId, request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/review")
    @Operation(summary = "更新复习状态", description = "调整复习状态、下次复习时间，并记录最近复习时间。")
    public Result<Void> updateReview(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId,
            @RequestBody NoteReviewDTO request) {
        try {
            noteService.updateReview(userId, noteId, request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/summary")
    @Operation(summary = "创建摘要作业", description = "为指定笔记提交异步摘要生成任务。")
    public Result<AiAsyncJobDTO> generateSummary(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            return Result.success(noteService.generateSummary(userId, noteId));
        } catch (Exception e) {
            log.error("Failed to generate note summary", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/organize")
    @Operation(summary = "创建整理作业", description = "为指定笔记提交异步整理任务，返回标题和标签建议。")
    public Result<AiAsyncJobDTO> organizeNote(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            return Result.success(noteService.organizeNote(userId, noteId));
        } catch (Exception e) {
            log.error("Failed to organize note", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/extract-tasks")
    @Operation(summary = "创建任务提取作业", description = "从笔记中异步提取可执行任务建议。")
    public Result<AiAsyncJobDTO> extractTasks(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            return Result.success(noteService.extractTasks(userId, noteId));
        } catch (Exception e) {
            log.error("Failed to extract tasks from note", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/internal/{noteId}/summary")
    @Hidden
    public Result<Void> updateSummary(@PathVariable("noteId") Long noteId,
            @RequestBody NoteInternalUpdateSummaryDTO request) {
        try {
            noteService.updateSummary(request.getUserId(), noteId, request.getSummary());
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to update note summary", e);
            return Result.error(e.getMessage());
        }
    }
}
