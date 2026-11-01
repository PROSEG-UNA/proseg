package com.proseg.msvc_auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_auth.dto.KeycloakUserResponseDto;
import com.proseg.msvc_auth.exception.UserException;
import com.proseg.msvc_auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminServiceTest {
    private static final String BASE = "http://keycloak.test";
    private static final String TOKEN_URL = BASE + "/realms/master/protocol/openid-connect/token";
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private UserRepository userRepository;
    private KeycloakAdminService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        userRepository = mock(UserRepository.class);
        service = new KeycloakAdminService(restTemplate, new ObjectMapper(), userRepository);
        ReflectionTestUtils.setField(service, "keycloakServerUrl", BASE);
        ReflectionTestUtils.setField(service, "realm", "test-realm");
        ReflectionTestUtils.setField(service, "adminUsername", "fixture-admin");
        ReflectionTestUtils.setField(service, "adminPassword", "fixture-password");
    }

    @Test
    void getsUserAndMapsProfileImageAttribute() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1",
                "{\"id\":\"u-1\",\"username\":\"alice\",\"email\":\"alice@example.test\","
                        + "\"firstName\":\"Alice\",\"lastName\":\"Test\","
                        + "\"attributes\":{\"profileImageObjectName\":[\" avatar.png \"]}}");

        KeycloakUserResponseDto result = service.getUserById("u-1");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getProfileImageObjectName()).isEqualTo("avatar.png");
        assertThat(result.getProfileImageUrl()).isEqualTo("/api/v1/archive/files/avatar.png");
        server.verify();
    }

    @Test
    void registersUserAndSetsPasswordOnlyAfterCreation() {
        expectAdminToken();
        HttpHeaders createdHeaders = new HttpHeaders();
        createdHeaders.setLocation(URI.create(BASE + "/admin/realms/test-realm/users/created-1"));
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED).headers(createdHeaders));
        expectAdminToken();
        expect(HttpMethod.PUT, BASE + "/admin/realms/test-realm/users/created-1/reset-password", "");

        assertThat(service.registerUser("alice", "alice@example.test", "TestPassword1!", "Alice", "Test"))
                .isEqualTo("created-1");
        server.verify();
    }

    @Test
    void missingLocationOnRegistrationFailsClearly() {
        expectAdminToken();
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> service.registerUser("alice", "alice@example.test", null, "Alice", "Test", false))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Keycloak no retorno Location header al crear el usuario");
        server.verify();
    }

    @Test
    void filtersInternalAndCompositeRolesAndPaginatesCompositeResults() {
        String roleJson = "["
                + "{\"id\":\"1\",\"name\":\"VIEW\",\"composite\":false,\"attributes\":{\"domain\":[\"AUTH\"]}},"
                + "{\"id\":\"2\",\"name\":\"default-roles-test\",\"composite\":false},"
                + "{\"id\":\"3\",\"name\":\"GROUP\",\"composite\":true},"
                + "{\"id\":\"4\",\"name\":\"offline_access\",\"composite\":true}]";
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles?briefRepresentation=false", roleJson);
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles", roleJson);

        assertThat(service.getBaseRoles()).hasSize(1)
                .first().satisfies(role -> {
                    assertThat(role.getName()).isEqualTo("VIEW");
                    assertThat(role.getDomain()).isEqualTo("AUTH");
                });

        var page = service.getCompositeRoles(PageRequest.of(0, 1));
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getName()).isEqualTo("GROUP");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.last()).isTrue();
        server.verify();
    }

    @Test
    void returnsUsersAndLocalStatusForPagedUserListing() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?first=0&max=2",
                "[{\"id\":\"u-1\",\"username\":\"alice\",\"attributes\":{\"profileImageObjectName\":\"profile.png\"}},"
                        + "{\"id\":\"u-2\",\"username\":\"bob\"}]");
        when(userRepository.findAllByKeycloakUserIdIn(anyList())).thenReturn(List.of());
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/count", "3");

        var page = service.getAllUsers(PageRequest.of(0, 2));

        assertThat(page.content()).extracting(KeycloakUserResponseDto::getUsername).containsExactly("alice", "bob");
        assertThat(page.content()).extracting(KeycloakUserResponseDto::getStatus).containsExactly("PENDING", "PENDING");
        assertThat(page.content().get(0).getProfileImageUrl()).isEqualTo("/api/v1/archive/files/profile.png");
        assertThat(page.totalElements()).isEqualTo(3);
        verify(userRepository).findAllByKeycloakUserIdIn(List.of("u-1", "u-2"));
        server.verify();
    }

    @Test
    void mapsMissingKeycloakUserToDomainException() {
        expectAdminToken();
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users/missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.getUserById("missing")).isInstanceOf(UserException.class);
        server.verify();
    }

    @Test
    void fetchesRoleMappingsAndKeepsOnlyNonInternalRoles() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1/role-mappings/realm",
                "[{\"id\":\"r1\",\"name\":\"VIEW\",\"description\":\"Read\",\"composite\":true},"
                        + "{\"id\":\"r2\",\"name\":\"offline_access\"}]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/MANAGER/composites",
                "[{\"id\":\"r1\",\"name\":\"VIEW\",\"composite\":false},"
                        + "{\"id\":\"r2\",\"name\":\"default-roles-test\"}]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/MANAGER/users",
                "[{\"id\":\"u-1\",\"username\":\"alice\"}]");

        assertThat(service.getRolesByUserId("u-1")).extracting(
                com.proseg.msvc_auth.dto.RoleResponseDto::getName).containsExactly("VIEW");
        assertThat(service.getRoleComposites("MANAGER")).extracting(
                com.proseg.msvc_auth.dto.RoleResponseDto::getName).containsExactly("VIEW");
        assertThat(service.getUsersByRole("MANAGER")).extracting(KeycloakUserResponseDto::getUsername)
                .containsExactly("alice");
        server.verify();
    }

    @Test
    void assignsAndRemovesRealmRolesUsingResolvedKeycloakRepresentations() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1/role-mappings/realm", "[]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles-by-id/r-1",
                "{\"id\":\"r-1\",\"name\":\"VIEW\"}");
        expect(HttpMethod.POST, BASE + "/admin/realms/test-realm/users/u-1/role-mappings/realm", "");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles-by-id/r-1",
                "{\"id\":\"r-1\",\"name\":\"VIEW\"}");
        expect(HttpMethod.DELETE, BASE + "/admin/realms/test-realm/users/u-1/role-mappings/realm", "");

        service.assignRoleToUser("u-1", "r-1");
        service.removeRoleFromUser("u-1", "r-1");

        server.verify();
    }

    @Test
    void findsEmailAndBulkUsersWhileIgnoringNotFoundIds() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?email=alice@example.test&exact=true",
                "[{\"id\":\"u-1\"}]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?email=missing@example.test&exact=true", "[]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1",
                "{\"id\":\"u-1\",\"username\":\"alice\"}");
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/missing", null, HttpStatus.NOT_FOUND);

        assertThat(service.findUserIdByEmail("alice@example.test")).isEqualTo("u-1");
        assertThatThrownBy(() -> service.findUserIdByEmail("missing@example.test"))
                .isInstanceOf(com.proseg.msvc_auth.exception.PasswordResetException.class);
        assertThat(service.getUsersByIds(java.util.Arrays.asList("u-1", "u-1", " ", null, "missing")))
                .extracting(KeycloakUserResponseDto::getId).containsExactly("u-1");
        server.verify();
    }

    @Test
    void preservesExistingUserFieldsWhenEnablingAndPreventsDeletingSuperAdmin() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1",
                "{\"username\":\"alice\",\"firstName\":\"Alice\",\"email\":\"alice@example.test\","
                        + "\"enabled\":false,\"attributes\":{\"department\":[\"test\"]},"
                        + "\"requiredActions\":[\"UPDATE_PROFILE\"]}");
        expect(HttpMethod.PUT, BASE + "/admin/realms/test-realm/users/u-1", "");
        expectAdminToken();
        expect(HttpMethod.DELETE, BASE + "/admin/realms/test-realm/roles/TEST_ROLE", "");

        service.enableUser("u-1");
        assertThatThrownBy(() -> service.deleteRole("super_administrador"))
                .isInstanceOf(IllegalArgumentException.class);
        service.deleteRole("TEST_ROLE");
        server.verify();
    }

    @Test
    void createsCompositeRolesAndKeepsCurrentValuesWhenOptionalUpdatesAreBlank() {
        expectAdminToken();
        expect(HttpMethod.POST, BASE + "/admin/realms/test-realm/roles", "");
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/READ",
                "{\"id\":\"r-read\",\"name\":\"READ\"}");
        expect(HttpMethod.POST, BASE + "/admin/realms/test-realm/roles/MANAGER/composites", "");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/OLD",
                "{\"name\":\"OLD\",\"description\":\"Existing description\"}");
        expect(HttpMethod.PUT, BASE + "/admin/realms/test-realm/roles/OLD", "");

        service.createCompositeRole("MANAGER", " ", List.of("READ"));
        service.updateRole("OLD", " ", " ", List.of());

        server.verify();
    }

    @Test
    void reportsUserExistenceAndReturnsFalseOnRemoteError() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?username=alice", "[{\"id\":\"u-1\"}]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?username=missing", "[]");
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users?username=offline", null, HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(service.userExists("alice")).isTrue();
        assertThat(service.userExists("missing")).isFalse();
        assertThat(service.userExists("offline")).isFalse();
        server.verify();
    }

    @Test
    void updatesRoleMetadataAndReplacesCompositePrivileges() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/OLD",
                "{\"name\":\"OLD\",\"description\":\"Existing\"}");
        expect(HttpMethod.PUT, BASE + "/admin/realms/test-realm/roles/OLD", "");
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/RENAMED/composites",
                "[{\"id\":\"old\",\"name\":\"READ\"}]");
        expect(HttpMethod.DELETE, BASE + "/admin/realms/test-realm/roles/RENAMED/composites", "");
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/roles/WRITE",
                "{\"id\":\"new\",\"name\":\"WRITE\"}");
        expect(HttpMethod.POST, BASE + "/admin/realms/test-realm/roles/RENAMED/composites", "");

        service.updateRole("OLD", "RENAMED", null, List.of("WRITE"));

        server.verify();
    }

    @Test
    void updatesAndRemovesProfileImageAttributesWithoutLosingOtherUserFields() {
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1",
                "{\"id\":\"u-1\",\"username\":\"alice\",\"email\":\"alice@example.test\","
                        + "\"enabled\":true,\"attributes\":{\"department\":[\"operations\"],"
                        + "\"profileImageObjectName\":[\"old.png\"]},"
                        + "\"requiredActions\":[\"UPDATE_PROFILE\"]}");
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users/u-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"department\":[\"operations\"]")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        expectAdminToken();
        expect(HttpMethod.GET, BASE + "/admin/realms/test-realm/users/u-1",
                "{\"id\":\"u-1\",\"username\":\"alice\",\"email\":\"alice@example.test\","
                        + "\"attributes\":{\"department\":[\"operations\"]}}");

        KeycloakUserResponseDto result = service.updateProfileImageObjectName("u-1", "  ");

        assertThat(result.getProfileImageObjectName()).isNull();
        assertThat(result.getProfileImageUrl()).isNull();
        server.verify();
    }

    @Test
    void mapsRegistrationConflictsToUsernameOrEmailErrors() {
        expectAdminToken();
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"errorMessage\":\"username exists\"}"));
        expectAdminToken();
        server.expect(requestTo(BASE + "/admin/realms/test-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"errorMessage\":\"email already exists\"}"));

        assertThatThrownBy(() -> service.registerUser(
                "alice", "alice@example.test", null, "Alice", "Test", false))
                .isInstanceOf(UserException.class).hasMessageContaining("alice");
        assertThatThrownBy(() -> service.registerUser(
                "alice", "alice@example.test", null, "Alice", "Test", false))
                .isInstanceOf(UserException.class).hasMessageContaining("alice@example.test");
        server.verify();
    }

    private void expectAdminToken() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token-fixture\"}", MediaType.APPLICATION_JSON));
    }

    private void expect(HttpMethod method, String url, String body) {
        server.expect(requestTo(url)).andExpect(method(method))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expect(HttpMethod method, String url, String body, HttpStatus status) {
        server.expect(requestTo(url)).andExpect(method(method))
                .andRespond(withStatus(status).body(body == null ? "" : body));
    }
}
