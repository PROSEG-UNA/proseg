package com.proseg.msvc_auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TurnstileServiceTest {
    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TurnstileService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new TurnstileService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "secretKey", "test-secret");
        ReflectionTestUtils.setField(service, "maxRetries", 3);
    }

    @Test
    void validatesUsingMockedCloudflareEndpoint() {
        server.expect(requestTo(VERIFY_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"response\":\"captcha-fixture\"")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThat(service.validateCaptcha("captcha-fixture")).isTrue();
        server.verify();
    }

    @Test
    void rejectsBlankTokensAndNegativeVerificationResults() {
        assertThat(service.validateCaptcha("  ")).isFalse();
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThat(service.validateCaptcha("invalid-fixture")).isFalse();
        server.verify();
    }

    @Test
    void retriesTransientErrorsAndStopsAfterSuccessfulVerification() {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        server.expect(requestTo(VERIFY_URL))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThat(service.validateCaptcha("captcha-fixture")).isTrue();
        server.verify();
    }

    @Test
    void returnsFalseAfterRetryLimitIsReached() {
        for (int attempt = 0; attempt < 3; attempt++) {
            server.expect(requestTo(VERIFY_URL))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        }

        assertThat(service.validateCaptcha("captcha-fixture")).isFalse();
        server.verify();
    }
}
