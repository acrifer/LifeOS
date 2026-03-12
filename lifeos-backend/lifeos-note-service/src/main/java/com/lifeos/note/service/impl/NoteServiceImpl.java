package com.lifeos.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.ai.client.AiFeignClient;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.behavior.client.BehaviorFeignClient;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Resource
    private AiFeignClient aiFeignClient;

    @Resource
    private BehaviorFeignClient behaviorFeignClient;

    @Override
    public Long createNote(Long userId, NoteCreateDTO createDTO) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(createDTO.getTitle());
        note.setContent(createDTO.getContent());
        note.setTags(createDTO.getTags());

        this.save(note);
        recordBehaviorEvent(userId, "CREATE_NOTE", note.getId());
        return note.getId();
    }

    @Override
    public void updateNote(Long userId, NoteUpdateDTO updateDTO) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, updateDTO.getId()).eq(Note::getUserId, userId);

        Note note = this.getOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }

        if (updateDTO.getTitle() != null)
            note.setTitle(updateDTO.getTitle());
        if (updateDTO.getContent() != null)
            note.setContent(updateDTO.getContent());
        if (updateDTO.getTags() != null)
            note.setTags(updateDTO.getTags());
        if (updateDTO.getSummary() != null)
            note.setSummary(updateDTO.getSummary());
        boolean updated = this.update(note, wrapper);
        if (!updated) {
            throw new RuntimeException("Note update failed");
        }
        recordBehaviorEvent(userId, "UPDATE_NOTE", note.getId());
    }

    @Override
    public void deleteNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId).eq(Note::getUserId, userId);
        boolean removed = this.remove(wrapper);
        if (!removed) {
            throw new RuntimeException("Note not found or access denied");
        }
    }

    @Override
    public Note getNoteDetail(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId).eq(Note::getUserId, userId);
        Note note = this.getOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }
        recordBehaviorEvent(userId, "READ_NOTE", noteId);
        return note;
    }

    @Override
    public List<Note> listUserNotes(Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId).orderByDesc(Note::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<Note> searchNotes(Long userId, String keyword) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Note::getTitle, keyword)
                    .or()
                    .like(Note::getContent, keyword)
                    .or()
                    .like(Note::getTags, keyword));
        }

        wrapper.orderByDesc(Note::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public void updateSummary(Long userId, Long noteId, String summary) {
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .set(Note::getSummary, summary);
        if (!this.update(wrapper)) {
            throw new RuntimeException("Note summary update failed");
        }
    }

    @Override
    public AiSummaryResult generateSummary(Long userId, Long noteId) {
        Note note = getOwnedNote(userId, noteId);
        AiSummaryCommand command = new AiSummaryCommand();
        command.setNoteId(note.getId());
        command.setUserId(note.getUserId());
        command.setTitle(note.getTitle());
        command.setContent(note.getContent());
        return aiFeignClient.generateNoteSummary(command).getData();
    }

    private void recordBehaviorEvent(Long userId, String actionType, Long targetId) {
        try {
            BehaviorEventCommand command = new BehaviorEventCommand();
            command.setUserId(userId);
            command.setActionType(actionType);
            command.setTargetId(targetId);
            behaviorFeignClient.recordEvent(command);
        } catch (Exception ex) {
            log.warn("Failed to record behavior event {} for user {}", actionType, userId, ex);
        }
    }

    private Note getOwnedNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId).eq(Note::getUserId, userId);
        Note note = this.getOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }
        return note;
    }
}
