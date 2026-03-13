package com.lifeos.behavior.messaging;

import com.alibaba.fastjson2.JSON;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.behavior.service.BehaviorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(topic = BehaviorMqConstants.TOPIC, consumerGroup = BehaviorMqConstants.CONSUMER_GROUP)
public class BehaviorEventConsumer implements RocketMQListener<String> {

    @Resource
    private BehaviorService behaviorService;

    @Override
    public void onMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty behavior event payload");
            return;
        }
        BehaviorEventCommand command = JSON.parseObject(payload, BehaviorEventCommand.class);
        behaviorService.recordEvent(command);
    }
}
