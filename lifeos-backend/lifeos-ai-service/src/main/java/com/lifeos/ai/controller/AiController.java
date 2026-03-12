package com.lifeos.ai.controller;

import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiSummaryService aiSummaryService;

    @PostMapping("/note-summary")
    public Result<AiSummaryResult> generateNoteSummary(@RequestBody AiSummaryCommand command) {
        return Result.success(aiSummaryService.generateAndWriteBack(command));
    }
}
