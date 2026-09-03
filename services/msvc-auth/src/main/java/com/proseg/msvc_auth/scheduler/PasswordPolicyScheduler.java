package com.proseg.msvc_auth.scheduler;

import com.proseg.common.kafka.events.PasswordExpiringSoonEvent;
import com.proseg.common.kafka.topics.KafkaTopics;
import com.proseg.msvc_auth.entity.PasswordPolicy;
import com.proseg.msvc_auth.service.KeycloakAdminService;
import com.proseg.msvc_auth.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordPolicyScheduler {

    private final PasswordPolicyService passwordPolicyService;
    private final KeycloakAdminService keycloakAdminService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Costa_Rica") // 8:00am
    public void notifyExpiringPasswords() {
        List<PasswordPolicy> policies =
                passwordPolicyService.findPasswordsExpiringSoon(7);
        for (PasswordPolicy policy : policies) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(
                        Instant.now(),
                        policy.getExpiresAt()
                );
                if (!passwordPolicyService
                        .shouldSendExpirationNotification(daysLeft)) {
                    continue;
                }
                var user = keycloakAdminService
                        .getUserById(policy.getKeycloakUserId());
                kafkaTemplate.send(
                        KafkaTopics.PASSWORD_EXPIRING_SOON_TOPIC,
                        PasswordExpiringSoonEvent.builder()
                                .keycloakUserId(policy.getKeycloakUserId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .daysRemaining(daysLeft)
                                .expiresAt(policy.getExpiresAt())
                                .timestamp(System.currentTimeMillis())
                                .build()
                );
                log.info(
                        "Aviso de expiración enviado a userId={} — quedan {} días",
                        policy.getKeycloakUserId(),
                        daysLeft
                );
            } catch (Exception e) {
                log.error(
                        "Error enviando aviso de expiración para userId={}: {}",
                        policy.getKeycloakUserId(),
                        e.getMessage(),
                        e
                );
            }
        }
    }
}