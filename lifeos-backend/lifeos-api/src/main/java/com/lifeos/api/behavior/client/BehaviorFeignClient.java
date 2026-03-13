package com.lifeos.api.behavior.client;

import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lifeos-behavior-service")
public interface BehaviorFeignClient {

    @PostMapping("/behavior/internal/event")
    Result<Void> recordEvent(@RequestBody BehaviorEventCommand command);

    @GetMapping("/behavior/internal/dashboard")
    Result<DashboardStatsDTO> getDashboardStats(@RequestParam("userId") Long userId);
}
