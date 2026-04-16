package com.sssi.common.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    private String userId;
    private long timestamp;
    private String identifier;
}