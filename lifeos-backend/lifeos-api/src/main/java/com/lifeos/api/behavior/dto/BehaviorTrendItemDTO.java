package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BehaviorTrendItemDTO implements Serializable {
    private String date;
    private Long count;
}
