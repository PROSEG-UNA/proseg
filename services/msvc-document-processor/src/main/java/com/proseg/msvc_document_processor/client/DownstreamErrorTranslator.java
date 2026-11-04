package com.proseg.msvc_document_processor.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import org.springframework.http.HttpStatus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public final class DownstreamErrorTranslator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DownstreamErrorTranslator() {
    }

    public static DocumentProcessorException translate(
            FeignException exception,
            Supplier<DocumentProcessorException> unavailable,
            String defaultErrorCode
    ) {
        Optional<ByteBuffer> body = exception.responseBody();
        if (body.isEmpty()) {
            return unavailable.get();
        }
        try {
            String json = StandardCharsets.UTF_8.decode(body.get()).toString();
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String message = node.hasNonNull("message") ? node.get("message").asText() : null;
            if (message == null || message.isBlank()) {
                return unavailable.get();
            }
            String errorCode = defaultErrorCode;
            JsonNode errors = node.get("errors");
            if (errors != null && errors.isArray() && !errors.isEmpty() && errors.get(0).isTextual()) {
                errorCode = errors.get(0).asText();
            }
            HttpStatus status = resolveStatus(node, exception.status());
            return new DocumentProcessorException(status, errorCode, message);
        } catch (Exception parsingException) {
            return unavailable.get();
        }
    }

    private static HttpStatus resolveStatus(JsonNode node, int fallbackStatus) {
        int statusValue = node.hasNonNull("status") ? node.get("status").asInt(fallbackStatus) : fallbackStatus;
        HttpStatus resolved = HttpStatus.resolve(statusValue);
        return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
    }
}
