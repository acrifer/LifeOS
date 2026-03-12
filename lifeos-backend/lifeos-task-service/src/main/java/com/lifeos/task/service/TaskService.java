package com.lifeos.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.dto.TaskUpdateDTO;
import com.lifeos.task.domain.entity.Task;

import java.util.List;

public interface TaskService extends IService<Task> {

    Long createTask(Long userId, TaskCreateDTO createDTO);

    void updateTask(Long userId, TaskUpdateDTO updateDTO);

    void deleteTask(Long userId, Long taskId);

    List<Task> listUserTasks(Long userId);

    void completeTask(Long userId, Long taskId);
}
