package com.lifeos.task.controller;

import com.lifeos.common.response.Result;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.dto.TaskUpdateDTO;
import com.lifeos.task.domain.entity.Task;
import com.lifeos.task.service.TaskService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Resource
    private TaskService taskService;

    @PostMapping
    public Result<Long> createTask(@RequestHeader("X-User-Id") Long userId,
            @RequestBody TaskCreateDTO taskCreateDTO) {
        try {
            Long taskId = taskService.createTask(userId, taskCreateDTO);
            return Result.success(taskId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> updateTask(@RequestHeader("X-User-Id") Long userId,
            @RequestBody TaskUpdateDTO taskUpdateDTO) {
        try {
            taskService.updateTask(userId, taskUpdateDTO);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    public Result<Void> deleteTask(@RequestHeader("X-User-Id") Long userId,
            @PathVariable("taskId") Long taskId) {
        try {
            taskService.deleteTask(userId, taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Task>> listUserTasks(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<Task> tasks = taskService.listUserTasks(userId);
            return Result.success(tasks);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{taskId}/complete")
    public Result<Void> completeTask(@RequestHeader("X-User-Id") Long userId,
            @PathVariable("taskId") Long taskId) {
        try {
            taskService.completeTask(userId, taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
