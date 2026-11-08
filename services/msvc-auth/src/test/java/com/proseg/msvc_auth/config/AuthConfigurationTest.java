package com.proseg.msvc_auth.config;

import com.proseg.common.kafka.topics.KafkaTopics;
import com.proseg.msvc_auth.security.Privileges;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthConfigurationTest {
    @Test
    void createsAllKafkaTopicsWithRetentionAndReplicationSettings() {
        KafkaTopicConfig config = new KafkaTopicConfig();
        List<org.apache.kafka.clients.admin.NewTopic> topics = List.of(
                config.userLoginTopic(), config.userRegisteredTopic(), config.userAdminCreatedTopic(),
                config.userInvitedTopic(), config.managedUserCreatedTopic(), config.userPasswordConfiguredTopic(),
                config.passwordResetRequestedTopic(), config.passwordChangedTopic(),
                config.passwordExpiringSoonTopic(), config.passwordExpiredResetRequiredTopic());

        assertThat(topics).hasSize(10).allSatisfy(topic -> {
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicasAssignments()).isNull();
            assertThat(topic.configs()).containsEntry(TopicConfig.RETENTION_MS_CONFIG, "86400000");
        });
        assertThat(topics).extracting(org.apache.kafka.clients.admin.NewTopic::name)
                .contains(KafkaTopics.USER_LOGIN_TOPIC, KafkaTopics.PASSWORD_CHANGED_TOPIC);
    }

    @Test
    void createsKafkaAndRestTemplateBeansAndExposesPrivilegeCatalog() {
        KafkaProducerConfig producerConfig = new KafkaProducerConfig();
        ReflectionTestUtils.setField(producerConfig, "bootstrapServers", "kafka.test:9092");
        var producerFactory = (DefaultKafkaProducerFactory<String, Object>) producerConfig.producerFactory();
        KafkaTemplate<String, Object> template = producerConfig.kafkaTemplate();
        assertThat(producerFactory.getConfigurationProperties())
                .containsEntry("bootstrap.servers", "kafka.test:9092");
        assertThat(template).isNotNull();

        RestTemplateConfig restTemplateConfig = new RestTemplateConfig();
        RestTemplate restTemplate = restTemplateConfig.restTemplate(new RestTemplateBuilder());
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplateConfig.objectMapper().findModules()).isNotEmpty();
        assertThat(List.of(Privileges.Role.READ_BASE, Privileges.User.CREATE, Privileges.Invitation.READ_PENDING))
                .containsExactly("LEER_ROLES_BASE", "CREAR_USUARIO", "LEER_INVITACIONES_PENDIENTES");
    }
}
