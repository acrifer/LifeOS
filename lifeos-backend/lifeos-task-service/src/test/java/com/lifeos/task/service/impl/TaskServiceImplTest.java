package com.lifeos.task.service.impl;

import com.alibaba.fastjson2.JSON;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.entity.Task;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Spy
    @InjectMocks
    private TaskServiceImpl taskService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void createTaskKeepsSourceNoteLink() {
        AtomicReference<Task> savedTask = new AtomicReference<>();
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(88L);
            savedTask.set(task);
            return true;
        }).when(taskService).save(any(Task.class));

        TaskCreateDTO createDTO = new TaskCreateDTO();
        createDTO.setTitle("Follow up");
        createDTO.setDescription("Turn note ideas into next actions.");
        createDTO.setSourceNoteId(203L);

        Long taskId = taskService.createTask(5L, createDTO);

        assertThat(taskId).isEqualTo(88L);
        assertThat(savedTask.get()).isNotNull();
        assertThat(savedTask.get().getSourceNoteId()).isEqualTo(203L);
        verify(stringRedisTemplate).delete("user:task:list:5");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(eq(BehaviorMqConstants.TOPIC), messageCaptor.capture(), anyLong());
        BehaviorEventCommand command = JSON.parseObject(messageCaptor.getValue(), BehaviorEventCommand.class);
        assertThat(command.getUserId()).isEqualTo(5L);
        assertThat(command.getActionType()).isEqualTo("EXTRACT_TASK_FROM_NOTE");
        assertThat(command.getTargetId()).isEqualTo(203L);
        assertThat(command.getEventId()).isNotBlank();
    }
}
