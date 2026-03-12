package com.lifeos.ai.service;

import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;

public interface AiSummaryService {
    AiSummaryResult generateAndWriteBack(AiSummaryCommand command);
}
