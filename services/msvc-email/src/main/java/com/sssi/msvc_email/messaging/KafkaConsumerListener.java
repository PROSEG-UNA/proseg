package com.sssi.msvc_email.messaging;

import com.sssi.common.kafka.events.UserLoginEvent;
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
            groupId = "msvc-email-group"
    )
    public void onUserLogin(UserLoginEvent event) {

        log.info("LOGIN EVENT RECIBIDO: {}", event);

        emailEventService.sendLoginEmail(event);
    }
}