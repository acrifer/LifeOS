package com.lifeos.note.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
@TableName("note")
public class Note {
    // For ShardingSphere, ID must be a unique distributed ID. We can use logic or
    // MybatisPlus ASSIGN_ID
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String title;

    private String content; // Markdown content

    private String tags; // Note tags

    private String summary; // AI Summary

    // Note tags can be stored in content or a separate table, but we will rely on
    // full-text search or tags field if added.
    // Assuming simple content search for now.

    private Date createTime;

    private Date updateTime;
}
