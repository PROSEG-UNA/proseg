package com.sssi.msvc_email.messaging;

import com.sssi.common.kafka.events.*;
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
            topics = KafkaTopics.USER_INVITED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "userInvitedListenerFactory"
    )
    public void onUserInvited(UserInvitedEvent event) {
        log.info("USER INVITED EVENT: {}", event);
        emailEventService.sendInvitationEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.MANAGED_USER_CREATED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "managedUserCreatedListenerFactory"
    )
    public void onManagedUserCreated(ManagedUserCreatedEvent event) {
        log.info("MANAGED USER CREATED EVENT: {}", event);
        emailEventService.sendManagedUserCreatedNotificationEmails(event);
    }

    @KafkaListener(
            topics = KafkaTopics.USER_PASSWORD_CONFIGURED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "userPasswordConfiguredListenerFactory"
    )
    public void onUserPasswordConfigured(UserPasswordConfiguredEvent event) {
        log.info("USER PASSWORD CONFIGURED EVENT: {}", event);
        emailEventService.sendPasswordConfiguredEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PASSWORD_RESET_REQUESTED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "passwordResetRequestedListenerFactory"
    )
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("PASSWORD RESET REQUESTED EVENT: {}", event);
        emailEventService.sendPasswordResetEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PASSWORD_CHANGED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "passwordChangedListenerFactory"
    )
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("PASSWORD CHANGED EVENT: {}", event);
        emailEventService.sendPasswordChangedEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PASSWORD_EXPIRING_SOON_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "passwordExpiringSoonListenerFactory"
    )
    public void onPasswordExpiringSoon(PasswordExpiringSoonEvent event) {
        log.info("PASSWORD EXPIRING SOON EVENT: {}", event);
        emailEventService.sendPasswordExpiringSoonEmail(event);
    }

    @KafkaListener(
            topics = KafkaTopics.PASSWORD_EXPIRED_RESET_REQUIRED_TOPIC,
            groupId = "msvc-email-group",
            containerFactory = "passwordExpiredResetRequestedListenerFactory"
    )
    public void onPasswordExpiredResetRequested(PasswordExpiredResetRequiredEvent event) {
        log.info("PASSWORD EXPIRED RESET REQUESTED EVENT: {}", event);
        emailEventService.sendPasswordExpiredResetEmail(event);
    }
}