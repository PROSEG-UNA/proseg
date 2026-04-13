package com.sssi.msvc_auth.config;

import com.sssi.common.kafka.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic generateTopic() {

        Map<String, String> configurations = new HashMap<>();
        // TopicConfig.CLEANUP_POLICY_DELETE borra el mensaje despues de x tiempo
        configurations.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
        // Despues de un dia o se borra o se mantiene el mas actual
        configurations.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");
        // Tamaño maximo de cada segmento, 1 gigabyte
        configurations.put(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824");
        // Tamaño maximo de cada mensaje
        configurations.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1000012");

        return TopicBuilder.name(KafkaTopics.USER_LOGIN_EVENT)
                .partitions(2)
                .replicas(2)
                .configs(configurations)
                .build();
    }
}
