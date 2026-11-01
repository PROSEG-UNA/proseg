package com.proseg.msvc_auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.proseg.msvc_auth.config.SecurityConfig;
import com.proseg.msvc_auth.dto.KeycloakTokenDto;
import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.dto.RoleResponseDto;
import com.proseg.msvc_auth.entity.User;
import com.proseg.msvc_auth.exception.GlobalExceptionHandler;
import com.proseg.msvc_auth.exception.PasswordExpiredException;
import com.proseg.msvc_auth.security.Privileges;
import com.proseg.msvc_auth.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, RoleController.class, UserController.class},
        properties = {"routes.auth=/api/auth", "routes.role=/api/role", "routes.user=/api/user",
                "spring.cloud.config.enabled=false"})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, AuthMvcSecurityTest.JwtTestConfiguration.class})
class AuthMvcSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @org.springframework.beans.factory.annotation.Autowired private JwtDecoder jwtDecoder;

    @org.springframework.boot.test.mock.mockito.MockBean private KeycloakAuthService keycloakAuthService;
    @org.springframework.boot.test.mock.mockito.MockBean private KeycloakAdminService keycloakAdminService;
    @org.springframework.boot.test.mock.mockito.MockBean private UserApprobationService userApprobationService;
    @org.springframework.boot.test.mock.mockito.MockBean private PasswordPolicyService passwordPolicyService;
    @org.springframework.boot.test.mock.mockito.MockBean private KafkaTemplate<String, Object> kafkaTemplate;
    @org.springframework.boot.test.mock.mockito.MockBean private TurnstileService turnstileService;
    @org.springframework.boot.test.mock.mockito.MockBean private PasswordResetService passwordResetService;
    @org.springframework.boot.test.mock.mockito.MockBean private UserService userService;
    @org.springframework.boot.test.mock.mockito.MockBean private RoleService roleService;

    @Test
    void protectedRoleEndpointRequiresVerifiedTokenAndSpecificPrivilege() throws Exception {
        mockMvc.perform(get("/api/role/base"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/role/base").cookie(new jakarta.servlet.http.Cookie(
                        "auth_token", signedToken(List.of("OTHER_PRIVILEGE")))))
                .andExpect(status().isForbidden());

        when(roleService.getBaseRoles()).thenReturn(List.of(RoleResponseDto.builder()
                .id("role-1").name(Privileges.Role.READ_BASE).composite(false).build()));
        mockMvc.perform(get("/api/role/base").cookie(new jakarta.servlet.http.Cookie(
                        "auth_token", signedToken(List.of(Privileges.Role.READ_BASE)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value(Privileges.Role.READ_BASE));
        verify(roleService).getBaseRoles();
    }

    @Test
    void currentUserLookupUsesAuthenticatedSubjectInsteadOfConflictingCookie() throws Exception {
        String authenticatedSubject = "signed-test-user";
        String forgedCookie = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJmb3JnZWQtdXNlciJ9.invalid-signature";
        when(userService.getKeycloakUserById(authenticatedSubject)).thenReturn(
                KeycloakUserResponseDto.builder().id(authenticatedSubject).username("authenticated-user").build());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + signedToken(List.of()))
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", forgedCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(authenticatedSubject))
                .andExpect(jsonPath("$.data.username").value("authenticated-user"));

        verify(userService).getKeycloakUserById(authenticatedSubject);
        verify(userService, never()).getKeycloakUserById("forged-user");
        verify(keycloakAuthService, never()).extractUserIdFromToken(any());
    }

    @Test
    void loginIsPublicAndSetsHttpOnlyStrictCookies() throws Exception {
        when(keycloakAuthService.getToken("alice", "SafePass123!"))
                .thenReturn(KeycloakTokenDto.builder().accessToken("access-fixture")
                        .refreshToken("refresh-fixture").build());
        when(keycloakAuthService.extractUserIdFromToken("access-fixture")).thenReturn("kc-user");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice\",\"password\":\"SafePass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.hasItems(
                                org.hamcrest.Matchers.containsString("auth_token=access-fixture"),
                                org.hamcrest.Matchers.containsString("refresh_token=refresh-fixture"))))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(userApprobationService).assertUserIsApproved("kc-user");
        verify(passwordPolicyService).assertPasswordNotExpired("kc-user");
    }

    @Test
    void publicForgotPasswordStillValidatesItsRequest() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.test\"}"))
                .andExpect(status().isOk());
        verify(passwordResetService).requestPasswordReset("person@example.test");
    }

    @Test
    void authCookieEndpointsRefreshLogoutAndRegister() throws Exception {
        when(keycloakAuthService.extractUserIdFromToken(any())).thenReturn("kc-user");
        when(userService.getKeycloakUserById(any())).thenReturn(KeycloakUserResponseDto.builder()
                .id("kc-user").username("alice").build());
        mockMvc.perform(get("/api/auth").cookie(new jakarta.servlet.http.Cookie(
                        "auth_token", signedToken(List.of()))))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth Service is running"));
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + signedToken(List.of()))
                        .cookie(new jakarta.servlet.http.Cookie("auth_token",
                                "eyJhbGciOiJub25lIn0.eyJzdWIiOiJmb3JnZWQtdXNlciJ9.invalid-signature"))
                        .cookie(new jakarta.servlet.http.Cookie("ignored", "value")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
        verify(userService).getKeycloakUserById("signed-test-user");
        verify(keycloakAuthService, never()).extractUserIdFromToken(any());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-fixture")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.hasItems(
                                org.hamcrest.Matchers.containsString("auth_token="),
                                org.hamcrest.Matchers.containsString("refresh_token="))));
        verify(keycloakAuthService).logout("refresh-fixture");

        when(keycloakAuthService.refreshToken("refresh-fixture"))
                .thenReturn(KeycloakTokenDto.builder().accessToken("new-access").refreshToken("new-refresh").build());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-fixture")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());

        when(turnstileService.validateCaptcha("captcha-fixture")).thenReturn(true);
        when(keycloakAdminService.registerUser("alice", "alice@example.test", "SafePass123!", "Alice", "Test"))
                .thenReturn("new-user");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@example.test\","
                                + "\"password\":\"SafePass123!\",\"firstName\":\"Alice\","
                                + "\"lastName\":\"Test\",\"captchaToken\":\"captcha-fixture\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("alice"));
        verify(userApprobationService).createPendingUser("new-user");
        verify(passwordPolicyService).recordPasswordChange("new-user");
    }

    @Test
    void passwordAndInvitationEndpointsReachTheirServices() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-fixture\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/set-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invite-fixture\",\"password\":\"NewPass123!\","
                                + "\"confirmPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/invitation-info").param("token", "invite-fixture"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/123/resend-invitation")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token",
                                signedToken(List.of("AUTHENTICATED")))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/change-password")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token",
                                signedToken(List.of("AUTHENTICATED"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Current123!\",\"password\":\"NewPass123!\","
                                + "\"confirmPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());
        verify(passwordResetService).resetPassword("reset-fixture", "NewPass123!");
        verify(userService).activateUserWithToken("invite-fixture", "NewPass123!");
        verify(userService).getInvitationInfo("invite-fixture");
        verify(userService).resendInvitation("123");
        verify(passwordResetService).changePassword(
                "signed-test-user", "Current123!", "NewPass123!", "NewPass123!");
    }

    @Test
    void expiredPasswordLoginRequestsResetAndCaptchaFailureReturnsGenericServerError() throws Exception {
        when(keycloakAuthService.getToken("alice", "SafePass123!"))
                .thenReturn(KeycloakTokenDto.builder().accessToken("access-fixture")
                        .refreshToken("refresh-fixture").build());
        when(keycloakAuthService.extractUserIdFromToken("access-fixture")).thenReturn("kc-user");
        doThrow(PasswordExpiredException.expired())
                .when(passwordPolicyService).assertPasswordNotExpired("kc-user");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"alice\",\"password\":\"SafePass123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0]").value("PASSWORD_EXPIRED"));
        verify(passwordResetService).requestExpiredPasswordReset("kc-user");

        when(turnstileService.validateCaptcha("invalid-captcha")).thenReturn(false);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"alice@example.test\","
                                + "\"password\":\"SafePass123!\",\"firstName\":\"Alice\","
                                + "\"lastName\":\"Test\",\"captchaToken\":\"invalid-captcha\"}"))
                .andExpect(status().isInternalServerError());
        verify(keycloakAdminService, never()).registerUser(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void userAndRoleEndpointsEnforcePrivilegesAndDelegateOperations() throws Exception {
        String read = signedToken(List.of(Privileges.User.READ));
        String admin = signedToken(List.of(
                Privileges.User.READ_ALL, Privileges.User.CREATE, Privileges.User.APPROVE,
                Privileges.User.ASSIGN_ROLE, Privileges.User.REMOVE_ROLE, Privileges.User.READ,
                Privileges.User.READ_ROLES, Privileges.Role.CREATE, Privileges.Role.UPDATE,
                Privileges.Role.DELETE, Privileges.Role.READ_USERS_BY_ROLE,
                Privileges.Role.READ_COMPOSITE, Privileges.Role.READ_ROLE_COMPOSITES));
        mockMvc.perform(get("/api/user").cookie(new jakarta.servlet.http.Cookie("auth_token", read)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/statuses").cookie(new jakarta.servlet.http.Cookie("auth_token", read)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/keycloak/u-1").cookie(new jakarta.servlet.http.Cookie("auth_token", read)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/keycloak/batch").cookie(new jakarta.servlet.http.Cookie("auth_token", read))
                        .contentType(MediaType.APPLICATION_JSON).content("[\"u-1\"]"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/u-1/roles").cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user").cookie(new jakarta.servlet.http.Cookie("auth_token", admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\",\"email\":\"new@example.test\","
                                + "\"firstName\":\"New\",\"lastName\":\"User\"}"))
                .andExpect(status().isCreated());

        java.util.UUID id = java.util.UUID.randomUUID();
        when(userService.updateUserApproval(eq(id), eq(User.UserStatus.APPROVED), eq("signed-test-user")))
                .thenReturn(User.builder().id(id).keycloakUserId("kc-user").status(User.UserStatus.APPROVED).build());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/user/approval/" + id)
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/user/u-1/roles/role-1")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/user/u-1/roles/role-1")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/user/me/profile-image")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"objectName\":\"avatar.png\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/role/composite").cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/role/manager/composites").cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/role").cookie(new jakarta.servlet.http.Cookie("auth_token", admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"MANAGER\",\"description\":\"Manager role\","
                                + "\"privileges\":[\"READ\"]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/role/MANAGER")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"MANAGER_UPDATED\",\"description\":\"Updated\","
                                + "\"privileges\":[\"READ\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/role/MANAGER")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/role/MANAGER/users").cookie(new jakarta.servlet.http.Cookie("auth_token", admin)))
                .andExpect(status().isOk());
        verify(userService).getAllUsers(any());
        verify(userService).createManagedUser(any(), eq("signed-test-user"));
        verify(userService).updateCurrentUserProfileImage("signed-test-user", "avatar.png");
        verify(roleService).createCompositeRole("MANAGER", "Manager role", List.of("READ"));
        verify(roleService).updateRole(eq("MANAGER"), eq("MANAGER_UPDATED"), eq("Updated"), any());
        verify(roleService).deleteRole("MANAGER");
    }

    private String signedToken(List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("signed-test-user")
                .issuer("http://keycloak.test/realms/test-realm")
                .issuedAt(now.minusSeconds(1))
                .expiresAt(now.plusSeconds(60))
                .claim("realm_access", Map.of("roles", roles))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(JwtTestConfiguration.KEY.getKeyID()).build(),
                claims)).getTokenValue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        private static final RSAKey KEY = generateKey();

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY.toPublicKey()).build();
        }

        @Bean
        JwtEncoder jwtEncoder() {
            JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(KEY));
            return new NimbusJwtEncoder(source);
        }

        private static RSAKey generateKey() {
            try {
                return new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("auth-test-key").generate();
            } catch (com.nimbusds.jose.JOSEException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
