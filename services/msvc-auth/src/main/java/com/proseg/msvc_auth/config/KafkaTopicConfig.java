package com.proseg.msvc_auth.config;

import com.proseg.common.kafka.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    private static final Map<String, String> TOPIC_CONFIG = Map.of(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE,
            TopicConfig.RETENTION_MS_CONFIG,   "86400000",
            TopicConfig.SEGMENT_BYTES_CONFIG,  "1073741824",
            TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012"
    );

    @Bean
    public NewTopic userLoginTopic() {
        return TopicBuilder.name(KafkaTopics.USER_LOGIN_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(KafkaTopics.USER_REGISTERED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic userAdminCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.USER_ADMIN_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic userInvitedTopic() {
        return TopicBuilder.name(KafkaTopics.USER_INVITED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic managedUserCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.MANAGED_USER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic userPasswordConfiguredTopic() {
        return TopicBuilder.name(KafkaTopics.USER_PASSWORD_CONFIGURED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic passwordResetRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_RESET_REQUESTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic passwordChangedTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic passwordExpiringSoonTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_EXPIRING_SOON_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic passwordExpiredResetRequiredTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_EXPIRED_RESET_REQUIRED_TOPIC)
                .partitions(3)
                .replicas(1)
                .configs(TOPIC_CONFIG)
                .build();
    }
}