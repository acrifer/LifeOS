package com.lifeos.note.controller;

import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.NoteInternalUpdateSummaryDTO;
import com.lifeos.common.response.Result;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.service.NoteService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/note")
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping
    public Result<String> createNote(@RequestHeader("X-User-Id") Long userId,
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
    public Result<Void> updateNote(@RequestHeader("X-User-Id") Long userId,
            @RequestBody NoteUpdateDTO updateDTO) {
        try {
            noteService.updateNote(userId, updateDTO);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> deleteNote(@RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            noteService.deleteNote(userId, noteId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{noteId}")
    public Result<Note> getNoteDetail(@RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            Note note = noteService.getNoteDetail(userId, noteId);
            return Result.success(note);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Note>> listUserNotes(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<Note> notes = noteService.listUserNotes(userId);
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result<List<Note>> searchNotes(@RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "keyword", required = false) String keyword) {
        try {
            List<Note> notes = noteService.searchNotes(userId, keyword);
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/summary")
    public Result<AiSummaryResult> generateSummary(@RequestHeader("X-User-Id") Long userId,
            @PathVariable("noteId") Long noteId) {
        try {
            return Result.success(noteService.generateSummary(userId, noteId));
        } catch (Exception e) {
            log.error("Failed to generate note summary", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/internal/{noteId}/summary")
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
