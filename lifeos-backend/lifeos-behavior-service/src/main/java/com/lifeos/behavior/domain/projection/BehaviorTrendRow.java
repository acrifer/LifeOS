package com.lifeos.behavior.domain.projection;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BehaviorTrendRow {
    private LocalDate eventDate;
    private Long actionCount;
}
