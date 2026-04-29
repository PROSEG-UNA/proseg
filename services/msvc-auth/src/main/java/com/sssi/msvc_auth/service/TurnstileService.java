package com.sssi.msvc_auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${turnstile.secret-key}")
    private String secretKey;

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public boolean validateCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("Captcha token empty");
            return false;
        }

        try {
            String requestBody = String.format("{\"secret\":\"%s\",\"response\":\"%s\"}", secretKey, captchaToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(TURNSTILE_VERIFY_URL, request, String.class);

            if (response == null) {
                log.warn("Null response from Turnstile");
                return false;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);
            boolean success = jsonResponse.get("success").asBoolean(false);

            log.info("Validation of Turnstile: {}", success);
            return success;

        } catch (Exception e) {
            log.error("Error validating Turnstile captcha: {}", e.getMessage(), e);
            return false;
        }
    }
}
