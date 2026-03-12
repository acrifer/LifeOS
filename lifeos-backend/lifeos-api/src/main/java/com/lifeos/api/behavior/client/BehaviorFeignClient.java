package com.lifeos.api.behavior.client;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lifeos-behavior-service")
public interface BehaviorFeignClient {

    @PostMapping("/behavior/internal/event")
    Result<Void> recordEvent(@RequestBody BehaviorEventCommand command);
}
