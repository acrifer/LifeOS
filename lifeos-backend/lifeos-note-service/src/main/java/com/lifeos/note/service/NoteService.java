package com.lifeos.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;

import java.util.List;

public interface NoteService extends IService<Note> {

    Long createNote(Long userId, NoteCreateDTO createDTO);

    void updateNote(Long userId, NoteUpdateDTO updateDTO);

    void deleteNote(Long userId, Long noteId);

    Note getNoteDetail(Long userId, Long noteId);

    List<Note> listUserNotes(Long userId);

    List<Note> searchNotes(Long userId, String keyword);

    void updateSummary(Long userId, Long noteId, String summary);

    AiSummaryResult generateSummary(Long userId, Long noteId);
}
