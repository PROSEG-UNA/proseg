package com.proseg.msvc_email.notificacion.client;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_email.config.FeignConfig;
import com.proseg.msvc_email.notificacion.dto.KeycloakUserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "msvc-auth",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface AuthClient {

    @GetMapping("/user/keycloak/{id}")
    ApiResponse<KeycloakUserResponseDto> getUserById(@PathVariable String id);

    @GetMapping("/role/{roleName}/users")
    ApiResponse<List<KeycloakUserResponseDto>> getUsersByRole(@PathVariable String roleName);
}