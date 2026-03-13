package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiAsyncJobUpdateDTO implements Serializable {
    private String status;
    private String errorMessage;
    private AiAsyncJobResultDTO result;
}
