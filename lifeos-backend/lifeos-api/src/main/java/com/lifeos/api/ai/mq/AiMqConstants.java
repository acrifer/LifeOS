package com.lifeos.api.ai.mq;

public final class AiMqConstants {

    public static final String TOPIC = "lifeos-ai-job";
    public static final String CONSUMER_GROUP = "lifeos-ai-job-consumer";
    public static final long PRODUCER_TIMEOUT_MS = 3000L;

    private AiMqConstants() {
    }
}
