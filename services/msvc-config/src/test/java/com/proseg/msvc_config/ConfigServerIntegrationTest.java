package com.proseg.msvc_config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=native",
                "spring.cloud.config.server.native.search-locations=classpath:/test-configurations,classpath:/configurations"
        }
)
class ConfigServerIntegrationTest {

    private static final List<String> APPLICATIONS = List.of(
            "msvc-archive",
            "msvc-auth",
            "msvc-document-processor",
            "msvc-email",
            "msvc-gateway",
            "msvc-inventory",
            "msvc-maintenance",
            "msvc-transport"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void servesAllConfiguredServicesWithoutCallingExternalInfrastructure() throws Exception {
        for (String application : APPLICATIONS) {
            for (String profile : List.of("default", "prod")) {
                JsonNode environment = getEnvironment(application + "/" + profile);

                assertThat(environment.path("name").asText()).isEqualTo(application);
                assertThat(environment.path("propertySources")).isNotEmpty();
                assertThat(property(environment, "spring.application.name")).isEqualTo(application);
            }
        }
    }

    @Test
    void combinesSharedApplicationAndServicePropertiesWithServicePrecedence() throws Exception {
        JsonNode environment = getEnvironment("contract-fixture/prod");

        assertThat(property(environment, "contract.shared")).isEqualTo("shared-value");
        assertThat(property(environment, "contract.application-only")).isEqualTo("application-value");
        assertThat(property(environment, "contract.environment-only")).isEqualTo("prod-value");
        assertThat(property(environment, "contract.precedence")).isEqualTo("service-prod");
    }

    @Test
    void keepsDefaultAndProductionServiceProfilesSeparate() throws Exception {
        JsonNode defaultEnvironment = getEnvironment("msvc-auth/default");
        JsonNode productionEnvironment = getEnvironment("msvc-auth/prod");

        assertThat(property(defaultEnvironment, "logging.level.org.springframework.security"))
                .isEqualTo("DEBUG");
        assertThat(property(productionEnvironment, "logging.level.org.springframework.security"))
                .isEqualTo("${LOG_LEVEL_SPRING_SECURITY:INFO}");
        assertThat(property(defaultEnvironment, "spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .contains("keycloak:8080");
        assertThat(property(productionEnvironment, "spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .contains("auth.example.com");
    }

    @Test
    void fallsBackToSharedAndDefaultConfigurationForUnknownApplicationAndProfile() throws Exception {
        JsonNode unknownApplication = getEnvironment("unknown-service/prod");
        JsonNode unknownProfile = getEnvironment("msvc-auth/qa");

        assertThat(property(unknownApplication, "contract.shared")).isEqualTo("shared-value");
        assertThat(property(unknownProfile, "logging.level.org.springframework.security"))
                .isEqualTo("DEBUG");
        assertThat(property(unknownProfile, "spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .contains("keycloak:8080");
    }

    @Test
    void readsPropertiesFilesAndProfileActivatedYamlDocuments() throws Exception {
        JsonNode properties = getEnvironment("contract-properties/default");
        JsonNode yaml = getEnvironment("contract-multidoc/prod");

        assertThat(property(properties, "contract.properties-only")).isEqualTo("properties-value");
        assertThat(property(yaml, "contract.multi-base")).isEqualTo("base-value");
        assertThat(property(yaml, "contract.multi-profile")).isEqualTo("prod-value");
    }

    @Test
    void reportsServerErrorForMalformedConfigurationWithoutLeakingFixtureContents() {
        ResponseEntity<String> response = restTemplate.getForEntity("/malformed-fixture/default", String.class);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).doesNotContain("fixture-private-value");
    }

    @Test
    void currentlyServesConfigurationToAnonymousRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity("/msvc-auth/default", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"msvc-auth\"");
    }

    @Test
    void treatsActuatorHealthPathAsConfigServerApplicationAndProfile() throws Exception {
        JsonNode environment = getEnvironment("actuator/health");

        assertThat(environment.path("name").asText()).isEqualTo("actuator");
        assertThat(environment.path("profiles").get(0).asText()).isEqualTo("health");
        assertThat(property(environment, "contract.shared")).isEqualTo("shared-value");
    }

    private JsonNode getEnvironment(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/" + path, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotBlank();
        return objectMapper.readTree(response.getBody());
    }

    private String property(JsonNode environment, String name) {
        for (JsonNode source : environment.path("propertySources")) {
            JsonNode value = source.path("source").path(name);
            if (!value.isMissingNode()) {
                return value.asText();
            }
        }
        throw new AssertionError("Configuration property not found: " + name);
    }
}
