package com.sssi.msvc_email.messaging;

import com.sssi.common.kafka.events.UserLoginEvent;
import com.sssi.common.kafka.events.UserAdminCreatedEvent;
import com.sssi.common.kafka.events.UserRegisteredEvent;
import com.sssi.common.kafka.topics.KafkaTopics;
import com.sssi.msvc_email.notificacion.service.EmailEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerListener {

    private final EmailEventService emailEventService;

    @KafkaListener(
            topics = KafkaTopics.USER_LOGIN_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "userLoginListenerFactory"
    )
    public void onUserLogin(UserLoginEvent event) {
        log.info("LOGIN EVENT: {}", event);
        emailEventService.sendLoginEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.USER_REGISTERED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "userRegisteredListenerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("REGISTER EVENT: {}", event);
        emailEventService.sendRegisteredEmail(event);
        emailEventService.sendApprovalEmails(event);
    }

    @KafkaListener(
            topics = KafkaTopics.USER_ADMIN_CREATED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "userAdminCreatedListenerFactory"
    )
    public void onUserAdminCreated(UserAdminCreatedEvent event) {
        log.info("USER ADMIN CREATED EVENT: {}", event);
        emailEventService.sendAdminCreatedCredentialsEmail(event);
    }
}