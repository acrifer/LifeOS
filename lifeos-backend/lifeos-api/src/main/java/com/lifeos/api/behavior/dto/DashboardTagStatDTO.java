package com.lifeos.api.behavior.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardTagStatDTO implements Serializable {
    private String tag;
    private Long count;
}
