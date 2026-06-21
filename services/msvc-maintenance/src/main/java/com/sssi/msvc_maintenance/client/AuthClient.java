package com.sssi.msvc_maintenance.client;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_maintenance.config.FeignConfig;
import com.sssi.msvc_maintenance.dto.request.CreateManagedUserRequestDto;
import com.sssi.msvc_maintenance.dto.response.CreateManagedUserResponseDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserDto;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "msvc-auth",
        url = "${GATEWAY_BASE_URL:http://localhost:8081}/api",
        configuration = FeignConfig.class
)
public interface AuthClient {

    @GetMapping("/user/keycloak/{id}")
    ApiResponse<KeycloakUserDto> getUserById(@PathVariable String id);

    @GetMapping("/user/keycloak/{id}")
    ApiResponse<KeycloakUserResponse> findUserByKeycloakId(
            @PathVariable String id
    );

    @PostMapping("/user")
    ApiResponse<CreateManagedUserResponseDto> createManagedUser(@RequestBody CreateManagedUserRequestDto request);

        @GetMapping("/user")
        ApiResponse<PageResponse<KeycloakUserResponse>> findUsers(
                        @RequestParam("page") int page,
                        @RequestParam("size") int size,
                        @RequestParam(value = "search", required = false) String search
        );
}

