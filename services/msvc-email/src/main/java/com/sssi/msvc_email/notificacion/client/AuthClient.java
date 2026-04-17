package com.sssi.msvc_email.notificacion.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.msvc_email.notificacion.dto.KeycloakUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "msvc-auth"
)
public interface AuthClient {

    @GetMapping("/api/auth/users/keycloak/{id}")
    ApiResponse<KeycloakUserDto> getUserById(@PathVariable String id);

}