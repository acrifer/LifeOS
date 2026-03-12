package com.lifeos.task.domain.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TaskUpdateDTO {
    private Long id;
    private String title;
    private String description;
    private Date deadline;
    private String tags;
    private Integer status;
}
