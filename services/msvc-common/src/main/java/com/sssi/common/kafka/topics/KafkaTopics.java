package com.sssi.common.kafka.topics;

/**
 * Centralized Kafka topic definitions.
 * This class avoids hardcoding topic names across microservices,
 * ensuring consistency between producers and consumers.
 * All topics should be declared here and reused across the system.
 */
public final class KafkaTopics {

    /**
     * Private constructor to prevent instantiation.
     */
    private KafkaTopics() {
    }

    /**
     * Topic for user login events.
     * Produced by: msvc-auth
     * Consumed by: msvc-email (and other services if needed)
     */
    public static final String USER_LOGIN_TOPIC = "user-login";

    public static final String USER_REGISTERED_TOPIC = "user-registered";
}