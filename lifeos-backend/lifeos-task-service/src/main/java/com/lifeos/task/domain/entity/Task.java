package com.lifeos.task.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String title;

    private String description;

    private Date deadline;

    private String tags; // Comma separated tags

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceNoteId;

    // Status: 0-Pending, 1-In Progress, 2-Completed
    private Integer status;

    private Date createTime;
}
