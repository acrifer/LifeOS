package com.lifeos.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.lifeos.ai.config.AiProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.lifeos.api")
@EnableConfigurationProperties(AiProperties.class)
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
