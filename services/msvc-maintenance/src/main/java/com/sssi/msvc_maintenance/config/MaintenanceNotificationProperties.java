package com.sssi.msvc_maintenance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "maintenance.notification")
public class MaintenanceNotificationProperties {

    private List<String> extraEmails = List.of();
}
