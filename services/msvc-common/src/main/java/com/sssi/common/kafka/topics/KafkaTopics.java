package com.sssi.common.kafka.topics;

/**
 * Centralized Kafka topic definitions.
 * This class avoids hardcoding topic names across microservices,
 * ensuring consistency between producers and consumers.
 * All topics should be declared here and reused across the system.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String USER_LOGIN_TOPIC = "user-login-v1";

    public static final String USER_REGISTERED_TOPIC = "user-registered-v1";

    public static final String USER_ADMIN_CREATED_TOPIC = "user-admin-created-v1";

    public static final String USER_INVITED_TOPIC = "user-invited-v1";

    public static final String MANAGED_USER_CREATED_TOPIC = "managed-user-created-v1";

    public static final String USER_PASSWORD_CONFIGURED_TOPIC = "user-password-configured-topic";

    public static final String PASSWORD_RESET_REQUESTED_TOPIC = "password-reset-requested";

    public static final String PASSWORD_CHANGED_TOPIC = "password-changed-v1";

    public static final String PASSWORD_EXPIRING_SOON_TOPIC = "password-expiring-soon-v1";

    public static final String PASSWORD_EXPIRED_RESET_REQUIRED_TOPIC = "password-expired-reset-required-v1";

    public static final String USER_COMPANY_ASSIGNED_TOPIC = "user-company-assigned-v1";
}