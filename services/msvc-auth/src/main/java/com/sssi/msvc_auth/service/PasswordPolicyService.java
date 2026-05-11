package com.sssi.msvc_auth.service;

import com.sssi.msvc_auth.entity.PasswordPolicy;
import com.sssi.msvc_auth.exception.PasswordExpiredException;
import com.sssi.msvc_auth.repository.PasswordPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordPolicyService {

    @Value("${app.password.expiration-days:90}")
    private int expirationDays;

    @Value("${app.password.notification-days:7,3,1}")
    private String notificationDaysConfig;

    private final PasswordPolicyRepository passwordPolicyRepository;

    @Transactional
    public void recordPasswordChange(String keycloakUserId) {

        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationDays, ChronoUnit.DAYS);

        PasswordPolicy policy = passwordPolicyRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> PasswordPolicy.builder()
                        .keycloakUserId(keycloakUserId)
                        .build());

        policy.setLastChangedAt(now);
        policy.setExpiresAt(expiresAt);
        policy.setPolicyDays(expirationDays);

        passwordPolicyRepository.save(policy);

        log.info(
                "Política de contraseña registrada para userId={} — expira el {}",
                keycloakUserId,
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public void assertPasswordNotExpired(String keycloakUserId) {

        passwordPolicyRepository
                .findByKeycloakUserId(keycloakUserId)
                .ifPresent(policy -> {

                    if (Instant.now().isAfter(policy.getExpiresAt())) {

                        log.warn(
                                "Contraseña expirada para userId={} — expiró el {}",
                                keycloakUserId,
                                policy.getExpiresAt()
                        );

                        throw PasswordExpiredException.expired();
                    }

                    log.debug(
                            "Contraseña vigente para userId={} — expira el {}",
                            keycloakUserId,
                            policy.getExpiresAt()
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<PasswordPolicy> findPasswordsExpiringSoon(int daysBefore) {

        Instant now = Instant.now();
        Instant limit = now.plus(daysBefore, ChronoUnit.DAYS);

        return passwordPolicyRepository.findByExpiresAtBetween(
                now,
                limit
        );
    }

    @Transactional(readOnly = true)
    public boolean shouldSendExpirationNotification(long daysLeft) {

        Set<Long> notificationDays = Arrays.stream(
                        notificationDaysConfig.split(",")
                )
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        return notificationDays.contains(daysLeft);
    }
}