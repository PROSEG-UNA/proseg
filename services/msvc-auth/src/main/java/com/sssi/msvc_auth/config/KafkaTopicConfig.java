package com.sssi.msvc_auth.config;

import com.sssi.common.kafka.topics.KafkaTopics;
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
}