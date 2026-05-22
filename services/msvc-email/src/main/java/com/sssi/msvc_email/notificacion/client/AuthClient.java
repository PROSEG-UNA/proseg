package com.sssi.msvc_email.notificacion.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_email.config.FeignConfig;
import com.sssi.msvc_email.notificacion.dto.KeycloakUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "msvc-auth",
    url = "${GATEWAY_BASE_URL:http://msvc-gateway:8081}/api",
        configuration = FeignConfig.class
)
public interface AuthClient {

    @GetMapping("/user/keycloak/{id}")
    ApiResponse<KeycloakUserDto> getUserById(@PathVariable String id);

    @GetMapping("/role/{roleName}/users")
    ApiResponse<List<KeycloakUserDto>> getUsersByRole(@PathVariable String roleName);
}