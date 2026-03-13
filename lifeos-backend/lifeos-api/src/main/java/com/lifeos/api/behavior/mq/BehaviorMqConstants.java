package com.lifeos.api.behavior.mq;

public final class BehaviorMqConstants {

    public static final String TOPIC = "lifeos-behavior-event";
    public static final String CONSUMER_GROUP = "lifeos-behavior-service-group";
    public static final long PRODUCER_TIMEOUT_MS = 3000L;

    private BehaviorMqConstants() {
    }
}
