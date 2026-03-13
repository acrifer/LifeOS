package com.lifeos.behavior.config;

import org.apache.rocketmq.spring.autoconfigure.ListenerContainerConfiguration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ RocketMQAutoConfiguration.class, ListenerContainerConfiguration.class })
public class RocketMqSupportConfig {
}
