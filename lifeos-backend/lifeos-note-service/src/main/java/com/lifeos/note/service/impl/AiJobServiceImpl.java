package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiAsyncJobResultDTO;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.api.ai.dto.AiJobStatus;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.ai.mq.AiMqConstants;
import com.lifeos.api.behavior.client.BehaviorFeignClient;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.common.response.Result;
import com.lifeos.note.domain.entity.AiWorkflowJob;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.mapper.AiWorkflowJobMapper;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.service.AiJobService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AiJobServiceImpl extends ServiceImpl<AiWorkflowJobMapper, AiWorkflowJob> implements AiJobService {

    private static final Set<String> NOTE_JOB_TYPES = Set.of(
            AiJobType.SUMMARY,
            AiJobType.ORGANIZE,
            AiJobType.EXTRACT_TASKS);

    @Resource
    private NoteMapper noteMapper;

    @Resource
    private BehaviorFeignClient behaviorFeignClient;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public AiAsyncJobDTO submitNoteJob(Long userId, Long noteId, String jobType) {
        String normalizedJobType = normalizeJobType(jobType);
        if (!NOTE_JOB_TYPES.contains(normalizedJobType)) {
            throw new RuntimeException("Unsupported note job type");
        }

        Note note = getOwnedNote(userId, noteId);
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setNoteId(noteId);
        command.setJobType(normalizedJobType);
        command.setTitle(note.getTitle());
        command.setContent(note.getContent());
        command.setTags(note.getTags());
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO submitWeeklyReview(Long userId) {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setJobType(AiJobType.WEEKLY_REVIEW);
        command.setDashboard(fetchDashboardStats(userId));
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO getJob(Long userId, Long jobId) {
        return toDto(getOwnedJob(userId, jobId));
    }

    @Override
    public List<AiAsyncJobDTO> listJobs(Long userId, Long noteId, String jobType, Integer limit) {
        LambdaQueryWrapper<AiWorkflowJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiWorkflowJob::getUserId, userId)
                .orderByDesc(AiWorkflowJob::getCreateTime);
        if (noteId != null) {
            wrapper.eq(AiWorkflowJob::getNoteId, noteId);
        }
        if (StringUtils.hasText(jobType)) {
            wrapper.eq(AiWorkflowJob::getJobType, normalizeJobType(jobType));
        }
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 50));
        }
        return this.list(wrapper).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void updateJobStatus(Long jobId, AiAsyncJobUpdateDTO request) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new RuntimeException("Job status is required");
        }

        AiWorkflowJob job = baseMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }

        String normalizedStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (AiJobStatus.SUCCESS.equals(job.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<AiWorkflowJob> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .set(AiWorkflowJob::getStatus, normalizedStatus)
                .set(AiWorkflowJob::getErrorMessage, request.getErrorMessage());

        if (AiJobStatus.SUCCESS.equals(normalizedStatus)) {
            wrapper.set(AiWorkflowJob::getResultPayload, JSON.toJSONString(request.getResult()))
                    .set(AiWorkflowJob::getFinishedTime, new Date());
        }
        if (AiJobStatus.FAILED.equals(normalizedStatus)) {
            wrapper.set(AiWorkflowJob::getFinishedTime, new Date());
        }
        this.update(wrapper);

        if (AiJobStatus.SUCCESS.equals(normalizedStatus)) {
            applyJobResult(job, request.getResult());
        }
    }

    private AiAsyncJobDTO createAndDispatchJob(AiAsyncJobCommand command) {
        AiWorkflowJob job = new AiWorkflowJob();
        job.setId(IdWorker.getId());
        job.setUserId(command.getUserId());
        job.setNoteId(command.getNoteId());
        job.setJobType(command.getJobType());
        job.setStatus(AiJobStatus.PENDING);
        command.setJobId(job.getId());
        job.setRequestPayload(JSON.toJSONString(command));
        this.save(job);

        try {
            rocketMQTemplate.syncSend(AiMqConstants.TOPIC, JSON.toJSONString(command), AiMqConstants.PRODUCER_TIMEOUT_MS);
            return toDto(job);
        } catch (Exception ex) {
            log.warn("Failed to dispatch ai job {}", job.getId(), ex);
            markJobFailed(job.getId(), "Failed to enqueue AI job");
            throw new RuntimeException("Failed to enqueue AI job");
        }
    }

    private DashboardStatsDTO fetchDashboardStats(Long userId) {
        try {
            Result<DashboardStatsDTO> result = behaviorFeignClient.getDashboardStats(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch dashboard stats for weekly review", ex);
        }
        return new DashboardStatsDTO();
    }

    private AiAsyncJobDTO toDto(AiWorkflowJob job) {
        AiAsyncJobDTO dto = new AiAsyncJobDTO();
        dto.setId(job.getId());
        dto.setNoteId(job.getNoteId());
        dto.setNoteTitle(resolveNoteTitle(job));
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreateTime(job.getCreateTime());
        dto.setUpdateTime(job.getUpdateTime());
        dto.setFinishedTime(job.getFinishedTime());
        if (StringUtils.hasText(job.getResultPayload())) {
            dto.setResult(JSON.parseObject(job.getResultPayload(), AiAsyncJobResultDTO.class));
        }
        return dto;
    }

    private String resolveNoteTitle(AiWorkflowJob job) {
        if (job.getNoteId() == null || job.getUserId() == null) {
            return null;
        }

        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, job.getNoteId())
                .eq(Note::getUserId, job.getUserId());
        Note note = noteMapper.selectOne(wrapper);
        return note == null ? null : note.getTitle();
    }

    private Note getOwnedNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId);
        Note note = noteMapper.selectOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }
        return note;
    }

    private AiWorkflowJob getOwnedJob(Long userId, Long jobId) {
        LambdaQueryWrapper<AiWorkflowJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .eq(AiWorkflowJob::getUserId, userId);
        AiWorkflowJob job = this.getOne(wrapper);
        if (job == null) {
            throw new RuntimeException("Job not found or access denied");
        }
        return job;
    }

    private void applyJobResult(AiWorkflowJob job, AiAsyncJobResultDTO result) {
        if (result == null) {
            return;
        }

        if (AiJobType.SUMMARY.equals(job.getJobType()) && StringUtils.hasText(result.getSummary())) {
            LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Note::getId, job.getNoteId())
                    .eq(Note::getUserId, job.getUserId())
                    .set(Note::getSummary, result.getSummary());
            noteMapper.update(null, wrapper);
            return;
        }

        if (AiJobType.ORGANIZE.equals(job.getJobType())) {
            recordBehaviorEvent(job.getUserId(), "ORGANIZE_NOTE", job.getNoteId());
            return;
        }

        if (AiJobType.EXTRACT_TASKS.equals(job.getJobType())
                && result.getTasks() != null
                && !result.getTasks().isEmpty()) {
            recordBehaviorEvent(job.getUserId(), "EXTRACT_TASK_FROM_NOTE", job.getNoteId());
        }
    }

    private void markJobFailed(Long jobId, String message) {
        LambdaUpdateWrapper<AiWorkflowJob> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .set(AiWorkflowJob::getStatus, AiJobStatus.FAILED)
                .set(AiWorkflowJob::getErrorMessage, message)
                .set(AiWorkflowJob::getFinishedTime, new Date());
        this.update(wrapper);
    }

    private void recordBehaviorEvent(Long userId, String actionType, Long targetId) {
        if (userId == null || targetId == null) {
            return;
        }
        try {
            BehaviorEventCommand command = new BehaviorEventCommand();
            command.setEventId(UUID.randomUUID().toString());
            command.setUserId(userId);
            command.setActionType(actionType);
            command.setTargetId(targetId);
            rocketMQTemplate.syncSend(BehaviorMqConstants.TOPIC, JSON.toJSONString(command),
                    BehaviorMqConstants.PRODUCER_TIMEOUT_MS);
        } catch (Exception ex) {
            log.warn("Failed to record behavior event {} for ai job", actionType, ex);
        }
    }

    private String normalizeJobType(String jobType) {
        if (!StringUtils.hasText(jobType)) {
            throw new RuntimeException("Job type is required");
        }
        return jobType.trim().toUpperCase(Locale.ROOT);
    }
}
