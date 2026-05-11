package com.sssi.msvc_email.config;

import com.sssi.common.kafka.events.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerProps() {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,                   "msvc-email-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   JsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest"
        );
    }

    private <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType);
        deserializer.addTrustedPackages("*");
        deserializer.ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(),
                new StringDeserializer(),
                deserializer
        );
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(targetType));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserLoginEvent>
    userLoginListenerFactory() {
        return listenerFactory(UserLoginEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent>
    userRegisteredListenerFactory() {
        return listenerFactory(UserRegisteredEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserInvitedEvent>
    userInvitedListenerFactory() {
        return listenerFactory(UserInvitedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ManagedUserCreatedEvent>
    managedUserCreatedListenerFactory() {
        return listenerFactory(ManagedUserCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserPasswordConfiguredEvent>
    userPasswordConfiguredListenerFactory() {
        return listenerFactory(UserPasswordConfiguredEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PasswordResetRequestedEvent>
    passwordResetRequestedListenerFactory() {
        return listenerFactory(PasswordResetRequestedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PasswordChangedEvent>
    passwordChangedListenerFactory() {
        return listenerFactory(PasswordChangedEvent.class);
    }
}