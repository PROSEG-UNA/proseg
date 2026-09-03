package com.proseg.msvc_auth.service;

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

    @Value("${turnstile.max-retries:3}")
    private int maxRetries;
    
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public boolean validateCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("Captcha token empty");
            return false;
        }

        int attempt = 0;
        while (attempt < maxRetries) {
            attempt++;
            try {
                String requestBody = String.format("{\"secret\":\"%s\",\"response\":\"%s\"}", secretKey, captchaToken);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

                String response = restTemplate.postForObject(TURNSTILE_VERIFY_URL, request, String.class);

                if (response == null) {
                    log.warn("Null response from Turnstile (attempt {}/{})", attempt, maxRetries);
                    if (attempt < maxRetries) {
                        continue;
                    }
                    return false;
                }

                JsonNode jsonResponse = objectMapper.readTree(response);
                boolean success = jsonResponse.get("success").asBoolean(false);

                log.info("Validation of Turnstile: {} (attempt {}/{})", success, attempt, maxRetries);
                return success;

            } catch (Exception e) {
                log.warn("Error validating Turnstile captcha on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                if (attempt >= maxRetries) {
                    log.error("Max retries reached for Turnstile captcha validation", e);
                    return false;
                }
            }
        }
        return false;
    }
}
