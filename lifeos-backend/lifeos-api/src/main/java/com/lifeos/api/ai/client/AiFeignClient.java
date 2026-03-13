package com.lifeos.api.ai.client;

import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiNoteOrganizeResult;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.AiTaskExtractionResult;
import com.lifeos.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lifeos-ai-service")
public interface AiFeignClient {

    @PostMapping("/ai/note-summary")
    Result<AiSummaryResult> generateNoteSummary(@RequestBody AiSummaryCommand command);

    @PostMapping("/ai/note-organize")
    Result<AiNoteOrganizeResult> organizeNote(@RequestBody AiSummaryCommand command);

    @PostMapping("/ai/note-task-extract")
    Result<AiTaskExtractionResult> extractTasks(@RequestBody AiSummaryCommand command);
}
