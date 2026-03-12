package com.lifeos.task.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.behavior.client.BehaviorFeignClient;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.dto.TaskUpdateDTO;
import com.lifeos.task.domain.entity.Task;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BehaviorFeignClient behaviorFeignClient;

    private static final String TASK_LIST_KEY_PREFIX = "user:task:list:";

    @Override
    public Long createTask(Long userId, TaskCreateDTO createDTO) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(createDTO.getTitle());
        task.setDescription(createDTO.getDescription());
        task.setDeadline(createDTO.getDeadline());
        task.setTags(createDTO.getTags());
        task.setStatus(0); // 0-Pending

        this.save(task);

        // Clear Redis cache
        clearUserTaskCache(userId);
        return task.getId();
    }

    @Override
    public void updateTask(Long userId, TaskUpdateDTO updateDTO) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, updateDTO.getId()).eq(Task::getUserId, userId);

        Task task = this.getOne(wrapper);
        if (task == null) {
            throw new RuntimeException("Task not found or access denied");
        }

        if (updateDTO.getTitle() != null)
            task.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null)
            task.setDescription(updateDTO.getDescription());
        if (updateDTO.getDeadline() != null)
            task.setDeadline(updateDTO.getDeadline());
        if (updateDTO.getTags() != null)
            task.setTags(updateDTO.getTags());
        if (updateDTO.getStatus() != null)
            task.setStatus(updateDTO.getStatus());

        this.updateById(task);

        // Clear Redis cache
        clearUserTaskCache(userId);
    }

    @Override
    public void deleteTask(Long userId, Long taskId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getId, taskId).eq(Task::getUserId, userId);

        if (this.remove(wrapper)) {
            // Clear Redis cache
            clearUserTaskCache(userId);
        }
    }

    @Override
    public List<Task> listUserTasks(Long userId) {
        String cacheKey = TASK_LIST_KEY_PREFIX + userId;
        String cachedList = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cachedList != null) {
            return JSON.parseArray(cachedList, Task.class);
        }

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getUserId, userId).orderByDesc(Task::getCreateTime);

        List<Task> tasks = this.list(wrapper);

        // Set into Redis, valid for 1 hour
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(tasks), 1, TimeUnit.HOURS);

        return tasks;
    }

    @Override
    public void completeTask(Long userId, Long taskId) {
        TaskUpdateDTO updateDTO = new TaskUpdateDTO();
        updateDTO.setId(taskId);
        updateDTO.setStatus(2); // 2-Completed
        this.updateTask(userId, updateDTO);
        recordBehaviorEvent(userId, "FINISH_TASK", taskId);
    }

    private void clearUserTaskCache(Long userId) {
        stringRedisTemplate.delete(TASK_LIST_KEY_PREFIX + userId);
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
}
