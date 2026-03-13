package com.lifeos.behavior.service.impl;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.behavior.domain.entity.UserBehavior;
import com.lifeos.behavior.domain.projection.BehaviorTrendRow;
import com.lifeos.behavior.domain.projection.DashboardNoteRow;
import com.lifeos.behavior.mapper.BehaviorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorServiceImplTest {

    @Mock
    private BehaviorMapper behaviorMapper;

    private BehaviorServiceImpl behaviorService;

    @BeforeEach
    void setUp() {
        behaviorService = new BehaviorServiceImpl();
        ReflectionTestUtils.setField(behaviorService, "baseMapper", behaviorMapper);
    }

    @Test
    void recordEventNormalizesActionAndKeepsEventId() {
        BehaviorServiceImpl service = org.mockito.Mockito.spy(new BehaviorServiceImpl());
        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        doReturn(true).when(service).save(behaviorCaptor.capture());

        BehaviorEventCommand command = new BehaviorEventCommand();
        command.setEventId("event-101");
        command.setUserId(9L);
        command.setActionType(" read_note ");
        command.setTargetId(33L);

        service.recordEvent(command);

        assertThat(behaviorCaptor.getValue().getEventId()).isEqualTo("event-101");
        assertThat(behaviorCaptor.getValue().getActionType()).isEqualTo("READ_NOTE");
        assertThat(behaviorCaptor.getValue().getUserId()).isEqualTo(9L);
        assertThat(behaviorCaptor.getValue().getTargetId()).isEqualTo(33L);
    }

    @Test
    void recordEventIgnoresDuplicateEventId() {
        BehaviorServiceImpl service = org.mockito.Mockito.spy(new BehaviorServiceImpl());
        doThrow(new DuplicateKeyException("duplicate")).when(service).save(any(UserBehavior.class));

        BehaviorEventCommand command = new BehaviorEventCommand();
        command.setEventId("event-dup");
        command.setUserId(9L);
        command.setActionType("READ_NOTE");

        service.recordEvent(command);
    }

    @Test
    void getDashboardStatsBuildsKnowledgeOverview() {
        when(behaviorMapper.countPendingTasks(3L)).thenReturn(4L);
        when(behaviorMapper.countNotes(3L)).thenReturn(6L);
        when(behaviorMapper.countNotesToReview(3L)).thenReturn(2L);
        when(behaviorMapper.countNotesCreatedSince(eq(3L), any(LocalDateTime.class))).thenReturn(3L);
        when(behaviorMapper.countEventsSince(eq(3L), eq("ORGANIZE_NOTE"), any(LocalDateTime.class))).thenReturn(2L);
        when(behaviorMapper.countEventsSince(eq(3L), eq("FINISH_TASK"), any(LocalDateTime.class))).thenReturn(1L);
        when(behaviorMapper.countPendingExtractedTasks(3L)).thenReturn(2L);
        when(behaviorMapper.countAiInbox(3L)).thenReturn(3L);
        when(behaviorMapper.selectRecentNotes(3L, 60)).thenReturn(List.of(buildRecentNote(11L, "Redis notes",
                "redis,workflow", "summary", "REVIEW", true),
                buildRecentNote(12L, "Prompt cleanup", "workflow,ai", "", "EVERGREEN", false)));
        when(behaviorMapper.selectRecentTrend(eq(3L), any(LocalDateTime.class)))
                .thenReturn(List.of(buildTrend(LocalDate.now().minusDays(1), 4L)));

        DashboardStatsDTO stats = behaviorService.getDashboardStats(3L);

        assertThat(stats.getNotesToReviewCount()).isEqualTo(2L);
        assertThat(stats.getPendingExtractedTaskCount()).isEqualTo(2L);
        assertThat(stats.getAiInboxCount()).isEqualTo(3L);
        assertThat(stats.getRecentNotes()).hasSize(2);
        assertThat(stats.getTopTags()).extracting("tag").contains("workflow", "redis", "ai");
        assertThat(stats.getRecentTrend()).hasSize(7);
    }

    private DashboardNoteRow buildRecentNote(Long id, String title, String tags, String summary,
            String reviewState, boolean pinned) {
        DashboardNoteRow row = new DashboardNoteRow();
        row.setId(id);
        row.setTitle(title);
        row.setTags(tags);
        row.setSummary(summary);
        row.setReviewState(reviewState);
        row.setPinned(pinned);
        row.setUpdateTime(new Date());
        return row;
    }

    private BehaviorTrendRow buildTrend(LocalDate date, Long count) {
        BehaviorTrendRow row = new BehaviorTrendRow();
        row.setEventDate(date);
        row.setActionCount(count);
        return row;
    }
}
