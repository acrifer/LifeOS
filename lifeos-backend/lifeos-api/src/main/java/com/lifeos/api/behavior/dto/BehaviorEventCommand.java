package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BehaviorEventCommand implements Serializable {
    private Long userId;
    private String actionType;
    private Long targetId;
}
