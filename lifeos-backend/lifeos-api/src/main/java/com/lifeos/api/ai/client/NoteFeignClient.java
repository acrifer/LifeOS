package com.lifeos.api.ai.client;

import com.lifeos.api.ai.dto.NoteInternalUpdateSummaryDTO;
import com.lifeos.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lifeos-note-service")
public interface NoteFeignClient {

    @PostMapping("/note/internal/{noteId}/summary")
    Result<Void> updateSummary(@PathVariable("noteId") Long noteId,
            @RequestBody NoteInternalUpdateSummaryDTO request);
}
