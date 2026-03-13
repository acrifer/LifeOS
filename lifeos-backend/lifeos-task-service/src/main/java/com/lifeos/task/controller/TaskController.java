package com.lifeos.task.controller;

import com.lifeos.common.response.Result;
import com.lifeos.task.domain.dto.TaskCreateDTO;
import com.lifeos.task.domain.dto.TaskUpdateDTO;
import com.lifeos.task.domain.entity.Task;
import com.lifeos.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/task")
@Tag(name = "任务管理", description = "知识衍生任务和普通待办的增删改查")
public class TaskController {

    @Resource
    private TaskService taskService;

    @PostMapping
    @Operation(summary = "创建任务", description = "新建任务，支持关联来源笔记。")
    public Result<Long> createTask(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody TaskCreateDTO taskCreateDTO) {
        try {
            Long taskId = taskService.createTask(userId, taskCreateDTO);
            return Result.success(taskId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "更新任务", description = "更新任务标题、描述、状态或来源信息。")
    public Result<Void> updateTask(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody TaskUpdateDTO taskUpdateDTO) {
        try {
            taskService.updateTask(userId, taskUpdateDTO);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务", description = "删除当前用户的指定任务。")
    public Result<Void> deleteTask(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("taskId") Long taskId) {
        try {
            taskService.deleteTask(userId, taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取任务列表", description = "返回当前用户的全部任务列表。")
    public Result<List<Task>> listUserTasks(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            List<Task> tasks = taskService.listUserTasks(userId);
            return Result.success(tasks);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{taskId}/complete")
    @Operation(summary = "完成任务", description = "将指定任务标记为已完成。")
    public Result<Void> completeTask(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("taskId") Long taskId) {
        try {
            taskService.completeTask(userId, taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
