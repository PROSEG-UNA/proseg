package com.proseg.msvc_auth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_auth.exception.KeycloakException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MaintenanceCompanyClientTest {
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private MaintenanceCompanyClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new MaintenanceCompanyClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "gatewayBaseUrl", "http://gateway.test");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsCurrentJwtAndReadsCompanyFlag() {
        Jwt jwt = Jwt.withTokenValue("signed-fixture").header("alg", "RS256")
                .subject("user-1").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, "n/a"));
        server.expect(requestTo("http://gateway.test/api/v1/maintenance/user-companies/user-1/has-company"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer signed-fixture"))
                .andRespond(withSuccess("{\"data\":true}", MediaType.APPLICATION_JSON));

        assertThat(client.userHasCompany("user-1")).isTrue();
        server.verify();
    }

    @Test
    void omitsAuthorizationWithoutJwtAndReturnsFalseWhenDataMissing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a"));
        server.expect(requestTo("http://gateway.test/api/v1/maintenance/user-companies/user-2/has-company"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThat(client.userHasCompany("user-2")).isFalse();
        server.verify();
    }

    @Test
    void mapsRemoteFailuresToDomainException() {
        server.expect(requestTo("http://gateway.test/api/v1/maintenance/user-companies/user-3/has-company"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> client.userHasCompany("user-3"))
                .isInstanceOf(KeycloakException.class)
                .hasMessageContaining("empresa asociada");
        server.verify();
    }
}
