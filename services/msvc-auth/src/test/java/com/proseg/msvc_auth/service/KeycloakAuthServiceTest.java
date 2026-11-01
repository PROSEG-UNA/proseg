package com.proseg.msvc_auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_auth.exception.AuthenticationException;
import com.proseg.msvc_auth.exception.TokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

class KeycloakAuthServiceTest {
    private static final String BASE = "http://keycloak.test";
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private KeycloakAuthService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new KeycloakAuthService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "keycloakServerUrl", BASE);
        ReflectionTestUtils.setField(service, "realm", "test-realm");
        ReflectionTestUtils.setField(service, "clientId", "test-client");
        ReflectionTestUtils.setField(service, "clientSecret", "test-secret");
    }

    @Test
    void exchangesCredentialsForTokens() {
        server.expect(requestTo(BASE + "/realms/test-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("username=alice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=password")))
                .andRespond(withSuccess("{\"access_token\":\"access\",\"refresh_token\":\"refresh\"}", MediaType.APPLICATION_JSON));

        assertThat(service.getToken("alice", "SafeTestPass1"))
                .satisfies(tokens -> {
                    assertThat(tokens.getAccessToken()).isEqualTo("access");
                    assertThat(tokens.getRefreshToken()).isEqualTo("refresh");
                });
        server.verify();
    }

    @Test
    void mapsDisabledAccountAndInvalidGrantErrors() {
        server.expect(requestTo(BASE + "/realms/test-realm/protocol/openid-connect/token"))
                .andRespond(withBadRequest().body("{\"error\":\"invalid_grant\",\"error_description\":\"Account disabled\"}"));

        assertThatThrownBy(() -> service.getToken("alice", "SafeTestPass1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("deshabilitada");
        server.verify();
    }

    @Test
    void mapsInvalidCredentialsAndMalformedTokenResponses() {
        String url = BASE + "/realms/test-realm/protocol/openid-connect/token";
        server.expect(requestTo(url))
                .andRespond(withSuccess("{\"error\":\"access_denied\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.getToken("alice", "SafeTestPass1"))
                .isInstanceOf(AuthenticationException.class);
        server.verify();

        server.reset();
        server.expect(requestTo(url)).andRespond(withSuccess("{\"unexpected\":\"body\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.getToken("alice", "SafeTestPass1"))
                .isInstanceOf(TokenException.class);
        server.verify();
    }

    @Test
    void mapsClientConfigurationErrorsWithoutReturningCredentials() {
        String url = BASE + "/realms/test-realm/protocol/openid-connect/token";
        server.expect(requestTo(url))
                .andRespond(withBadRequest().body("{\"error\":\"invalid_client\"}"));
        assertThatThrownBy(() -> service.getToken("alice", "SafeTestPass1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cliente de Keycloak");
        server.verify();

        server.reset();
        server.expect(requestTo(url))
                .andRespond(withBadRequest().body("{\"error\":\"unauthorized_client\","
                        + "\"error_description\":\"direct grants disabled\"}"));
        assertThatThrownBy(() -> service.getToken("alice", "SafeTestPass1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Direct Access Grants");
        server.verify();
    }

    @Test
    void refreshesTokensAndMapsExpiredRefreshToken() {
        String url = BASE + "/realms/test-realm/protocol/openid-connect/token";
        server.expect(requestTo(url))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andRespond(withSuccess("{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}", MediaType.APPLICATION_JSON));

        assertThat(service.refreshToken("refresh-fixture").getAccessToken()).isEqualTo("new-access");
        server.verify();

        server.reset();
        server.expect(requestTo(url))
                .andRespond(withBadRequest().body("{\"error\":\"invalid_grant\"}"));
        assertThatThrownBy(() -> service.refreshToken("expired-fixture"))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("expirado");
        server.verify();
    }

    @Test
    void extractsSubjectFromJwtShapedInputWithoutValidatingHeaderOrSignature() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"forged-user\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(service.extractUserIdFromToken(header + "." + payload + ".not-a-valid-signature"))
                .isEqualTo("forged-user");
        assertThatThrownBy(() -> service.extractUserIdFromToken("not-a-jwt"))
                .isInstanceOf(TokenException.class);
        String emptySubject = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\" \"}".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.extractUserIdFromToken("header." + emptySubject + ".signature"))
                .isInstanceOf(TokenException.class);
    }

    @Test
    void logoutSuppressesRemoteFailures() {
        server.expect(requestTo(BASE + "/realms/test-realm/protocol/openid-connect/logout"))
                .andRespond(withNoContent());

        service.logout("refresh-fixture");

        server.verify();
    }

    @Test
    void logoutSuppressesKeycloakAvailabilityFailure() {
        server.expect(requestTo(BASE + "/realms/test-realm/protocol/openid-connect/logout"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        service.logout("refresh-fixture");

        server.verify();
    }
}
