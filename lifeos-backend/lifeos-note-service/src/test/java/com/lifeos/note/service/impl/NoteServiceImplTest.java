package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.lifeos.api.ai.client.AiFeignClient;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NoteSearchQueryDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.domain.entity.NoteReviewState;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Note.class);
    }

    @Spy
    @InjectMocks
    private NoteServiceImpl noteService;

    @Mock
    private AiFeignClient aiFeignClient;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void createNoteAppliesKnowledgeDefaults() {
        AtomicReference<Note> savedNote = new AtomicReference<>();
        doAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setId(101L);
            savedNote.set(note);
            return true;
        }).when(noteService).save(any(Note.class));

        NoteCreateDTO createDTO = new NoteCreateDTO();
        createDTO.setTitle("Daily capture");
        createDTO.setContent("Remember to revisit architecture notes.");

        Long noteId = noteService.createNote(9L, createDTO);

        assertThat(noteId).isEqualTo(101L);
        assertThat(savedNote.get()).isNotNull();
        assertThat(savedNote.get().getUserId()).isEqualTo(9L);
        assertThat(savedNote.get().getPinned()).isFalse();
        assertThat(savedNote.get().getReviewState()).isEqualTo(NoteReviewState.NEW);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(eq(BehaviorMqConstants.TOPIC), messageCaptor.capture(), anyLong());
        BehaviorEventCommand command = JSON.parseObject(messageCaptor.getValue(), BehaviorEventCommand.class);
        assertThat(command.getUserId()).isEqualTo(9L);
        assertThat(command.getActionType()).isEqualTo("CREATE_NOTE");
        assertThat(command.getTargetId()).isEqualTo(101L);
        assertThat(command.getEventId()).isNotBlank();
    }

    @Test
    void searchNotesIncludesReviewableAndNeedsOrganizationFilters() {
        AtomicReference<LambdaQueryWrapper<Note>> wrapperRef = new AtomicReference<>();
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(0));
            return List.of();
        }).when(noteService).list(any(LambdaQueryWrapper.class));

        NoteSearchQueryDTO query = new NoteSearchQueryDTO();
        query.setKeyword("workflow");
        query.setReviewState("REVIEWABLE");
        query.setNeedsOrganization(true);
        query.setSort("review");

        noteService.searchNotes(7L, query);

        assertThat(wrapperRef.get()).isNotNull();
        assertThat(wrapperRef.get().getSqlSegment()).contains("review_state");
        assertThat(wrapperRef.get().getSqlSegment()).contains("summary");
        assertThat(wrapperRef.get().getSqlSegment()).contains("tags");
    }

    @Test
    void updateNoteUsesConditionalUpdateWithoutMutatingShardingKey() {
        Note existing = new Note();
        existing.setId(12L);
        existing.setUserId(9L);
        doReturn(existing).when(noteService).getOne(any(LambdaQueryWrapper.class));

        AtomicReference<LambdaUpdateWrapper<Note>> wrapperRef = new AtomicReference<>();
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(0));
            return true;
        }).when(noteService).update(any(LambdaUpdateWrapper.class));

        NoteUpdateDTO updateDTO = new NoteUpdateDTO();
        updateDTO.setId(12L);
        updateDTO.setContent("Updated note");
        updateDTO.setSummary("AI summary");

        noteService.updateNote(9L, updateDTO);

        assertThat(wrapperRef.get()).isNotNull();
        assertThat(wrapperRef.get().getSqlSet()).contains("content");
        assertThat(wrapperRef.get().getSqlSet()).contains("summary");
        assertThat(wrapperRef.get().getSqlSet()).doesNotContain("user_id");
        assertThat(wrapperRef.get().getSqlSegment()).contains("id");
        assertThat(wrapperRef.get().getSqlSegment()).contains("user_id");
    }
}
