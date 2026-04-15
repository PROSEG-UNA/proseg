package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a user logs into the system.
 * This event is sent via Kafka and consumed by other services
 * such as email notifications, auditing, or analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {

    /**
     * User email associated with the login.
     */
    private String email;

    /**
     * Timestamp of the login event in epoch milliseconds.
     */
    private long timestamp;
}