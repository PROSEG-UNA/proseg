package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.dto.request.CompanyRequestDto;
import com.proseg.msvc_maintenance.dto.request.CompanyUserRequestDto;
import com.proseg.msvc_maintenance.dto.request.CompanyUsersRequestDto;
import com.proseg.msvc_maintenance.dto.request.CreateManagedUserRequestDto;
import com.proseg.msvc_maintenance.dto.response.CompanyResponseDto;
import com.proseg.msvc_maintenance.dto.response.CreateManagedUserResponseDto;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.proseg.msvc_maintenance.dto.response.UserCompanyResponseDto;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.exception.CompanyException;
import com.proseg.msvc_maintenance.exception.UserCompanyException;
import com.proseg.msvc_maintenance.mapper.UserCompanyMapper;
import com.proseg.msvc_maintenance.service.CompanyService;
import com.proseg.msvc_maintenance.service.impl.CompanyUserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyService companyService;
    @Mock
    private CompanyUserManagementService companyUserManagementService;
    @Mock
    private UserCompanyMapper userCompanyMapper;

    private MockMvc mockMvc;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new CompanyController(companyService, companyUserManagementService, userCompanyMapper),
                "routes.companies",
                ControllerMockMvcSupport.COMPANIES);
        companyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST create devuelve 201")
    void create_cuandoNombreValido_retornaCreated() throws Exception {
        CompanyRequestDto body = CompanyRequestDto.builder().name("Empresa Test").build();
        CompanyResponseDto dto = CompanyResponseDto.builder().id(companyId).name("Empresa Test").build();
        ArgumentCaptor<CompanyRequestDto> captor = ArgumentCaptor.forClass(CompanyRequestDto.class);
        when(companyService.create(any())).thenReturn(dto);

        mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Empresa Test"));

        verify(companyService).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Empresa Test");
    }

    @Test
    @DisplayName("POST create propaga conflicto de nombre")
    void create_cuandoNombreDuplicado_retornaConflict() throws Exception {
        CompanyRequestDto body = CompanyRequestDto.builder().name("Duplicada").build();
        when(companyService.create(any())).thenThrow(CompanyException.duplicateName("Duplicada"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                409,
                "COMPANY_DUPLICATE_NAME");
    }

    @Test
    @DisplayName("POST managed user devuelve 201")
    void createManagedUser_cuandoDatosValidos_retornaCreated() throws Exception {
        CreateManagedUserRequestDto body = CreateManagedUserRequestDto.builder()
                .username("usuario1")
                .email("user@example.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        CreateManagedUserResponseDto dto = CreateManagedUserResponseDto.builder()
                .userId("kc-new")
                .build();
        when(companyService.createManagedUser(any())).thenReturn(dto);

        mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value("kc-new"));
    }

    @Test
    @DisplayName("POST managed user propaga error de invitación")
    void createManagedUser_cuandoFallaInvitacion_retornaBadRequest() throws Exception {
        CreateManagedUserRequestDto body = CreateManagedUserRequestDto.builder()
                .username("usuario1")
                .email("user@example.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        when(companyService.createManagedUser(any())).thenThrow(CompanyException.inviteUserFailed("Correo inválido"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                400,
                "COMPANY_USER_INVITATION_FAILED");
    }

    @Test
    @DisplayName("GET me usa subject del JWT")
    void findMyCompany_cuandoUsuarioTieneEmpresa_retornaOk() throws Exception {
        CompanyResponseDto dto = CompanyResponseDto.builder().id(companyId).build();
        when(companyService.findByKeycloakUserId("kc-user-subject")).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(companyId.toString()));
    }

    @Test
    @DisplayName("GET me propaga sin empresa asociada")
    void findMyCompany_cuandoSinEmpresa_retornaNotFound() throws Exception {
        when(companyService.findByKeycloakUserId("kc-user-subject")).thenThrow(CompanyException.noAssociatedCompany());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/me")),
                404,
                "COMPANY_NO_ASSOCIATED");
    }

    @Test
    @DisplayName("GET by id devuelve empresa")
    void findById_cuandoExiste_retornaOk() throws Exception {
        CompanyResponseDto dto = CompanyResponseDto.builder().id(companyId).name("Acme").build();
        when(companyService.findById(companyId)).thenReturn(dto);

        mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme"));
    }

    @Test
    @DisplayName("GET by id propaga not found")
    void findById_cuandoNoExiste_retornaNotFound() throws Exception {
        when(companyService.findById(companyId)).thenThrow(CompanyException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId)),
                404,
                "COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("GET users devuelve lista")
    void findUsers_cuandoHayUsuarios_retornaOk() throws Exception {
        KeycloakUserResponse user = new KeycloakUserResponse(
                "kc-1", "user1", "u@example.com", "Ana", "Perez", "ACTIVE", List.of());
        when(companyUserManagementService.findUsersByCompanyId(companyId)).thenReturn(List.of(user));

        mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("kc-1"));
    }

    @Test
    @DisplayName("GET users propaga not found de empresa")
    void findUsers_cuandoEmpresaNoExiste_retornaNotFound() throws Exception {
        when(companyUserManagementService.findUsersByCompanyId(companyId)).thenThrow(CompanyException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users")),
                404,
                "COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("GET technicians mapea user companies")
    void findTechnicians_cuandoHayTecnicos_retornaOk() throws Exception {
        UserCompany entity = UserCompany.builder().id(UUID.randomUUID()).build();
        UserCompanyResponseDto mapped = UserCompanyResponseDto.builder().id(entity.getId()).build();
        when(companyUserManagementService.findByCompanyId(companyId)).thenReturn(List.of(entity));
        when(userCompanyMapper.toResponse(entity)).thenReturn(mapped);

        mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(entity.getId().toString()));
    }

    @Test
    @DisplayName("GET technicians propaga error")
    void findTechnicians_cuandoFalla_retornaNotFound() throws Exception {
        when(companyUserManagementService.findByCompanyId(companyId)).thenThrow(CompanyException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/technicians")),
                404,
                "COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("POST assign user devuelve 201")
    void assignUser_cuandoValido_retornaCreated() throws Exception {
        CompanyUserRequestDto body = CompanyUserRequestDto.builder().keycloakUserId("kc-assign").build();
        UserCompany entity = UserCompany.builder().id(UUID.randomUUID()).build();
        UserCompanyResponseDto mapped = UserCompanyResponseDto.builder().id(entity.getId()).build();
        when(companyUserManagementService.assignUserToCompany(companyId, "kc-assign")).thenReturn(entity);
        when(userCompanyMapper.toResponse(entity)).thenReturn(mapped);

        mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(entity.getId().toString()));
    }

    @Test
    @DisplayName("POST assign user propaga duplicado")
    void assignUser_cuandoDuplicado_retornaConflict() throws Exception {
        CompanyUserRequestDto body = CompanyUserRequestDto.builder().keycloakUserId("kc-assign").build();
        when(companyUserManagementService.assignUserToCompany(companyId, "kc-assign"))
                .thenThrow(UserCompanyException.duplicateRelation());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                409,
                "USER_COMPANY_DUPLICATE");
    }

    @Test
    @DisplayName("POST batch assign devuelve lista creada")
    void assignUsers_cuandoValido_retornaCreated() throws Exception {
        CompanyUsersRequestDto body = CompanyUsersRequestDto.builder()
                .keycloakUserIds(List.of("kc-a", "kc-b"))
                .build();
        UserCompany uc = UserCompany.builder().id(UUID.randomUUID()).build();
        UserCompanyResponseDto mapped = UserCompanyResponseDto.builder().id(uc.getId()).build();
        when(companyUserManagementService.assignUsersToCompany(eq(companyId), any())).thenReturn(List.of(uc));
        when(userCompanyMapper.toResponse(uc)).thenReturn(mapped);

        mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].id").value(uc.getId().toString()));
    }

    @Test
    @DisplayName("POST batch assign propaga error")
    void assignUsers_cuandoFalla_retornaNotFound() throws Exception {
        CompanyUsersRequestDto body = CompanyUsersRequestDto.builder()
                .keycloakUserIds(List.of("kc-a"))
                .build();
        when(companyUserManagementService.assignUsersToCompany(eq(companyId), any()))
                .thenThrow(UserCompanyException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(post(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                404,
                "USER_COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("GET listado paginado devuelve empresas")
    void findAll_cuandoHayDatos_retornaOk() throws Exception {
        CompanyResponseDto dto = CompanyResponseDto.builder().id(companyId).build();
        when(companyService.findAll(eq(null), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(companyId.toString()));
    }

    @Test
    @DisplayName("GET listado propaga error")
    void findAll_cuandoFalla_retornaBadGateway() throws Exception {
        when(companyService.findAll(any(), any(), any())).thenThrow(CompanyException.userLookupFailed());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.COMPANIES)),
                502,
                "USER_LOOKUP_FAILED");
    }

    @Test
    @DisplayName("PUT update devuelve empresa")
    void update_cuandoValido_retornaOk() throws Exception {
        CompanyRequestDto body = CompanyRequestDto.builder().name("Nuevo nombre").build();
        CompanyResponseDto dto = CompanyResponseDto.builder().id(companyId).name("Nuevo nombre").build();
        when(companyService.update(eq(companyId), any())).thenReturn(dto);

        mockMvc.perform(put(ControllerMockMvcSupport.COMPANIES + "/" + companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nuevo nombre"));
    }

    @Test
    @DisplayName("PUT update propaga not found")
    void update_cuandoNoExiste_retornaNotFound() throws Exception {
        CompanyRequestDto body = CompanyRequestDto.builder().name("X").build();
        when(companyService.update(eq(companyId), any())).thenThrow(CompanyException.notFound());

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(put(ControllerMockMvcSupport.COMPANIES + "/" + companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerMockMvcSupport.objectMapper().writeValueAsString(body))),
                404,
                "COMPANY_NOT_FOUND");
    }

    @Test
    @DisplayName("DELETE elimina empresa")
    void delete_cuandoExiste_retornaOk() throws Exception {
        mockMvc.perform(delete(ControllerMockMvcSupport.COMPANIES + "/" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(companyService).delete(companyId);
    }

    @Test
    @DisplayName("DELETE propaga in use")
    void delete_cuandoEnUso_retornaBadRequest() throws Exception {
        org.mockito.Mockito.doThrow(CompanyException.inUse("Acme"))
                .when(companyService).delete(companyId);

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(delete(ControllerMockMvcSupport.COMPANIES + "/" + companyId)),
                400,
                "COMPANY_IN_USE");
    }

    @Test
    @DisplayName("DELETE unassign user devuelve ok")
    void unassignUser_cuandoExiste_retornaOk() throws Exception {
        mockMvc.perform(delete(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users/kc-remove"))
                .andExpect(status().isOk());

        verify(companyUserManagementService).removeUserFromCompany(companyId, "kc-remove");
    }

    @Test
    @DisplayName("DELETE unassign propaga not found")
    void unassignUser_cuandoNoExiste_retornaNotFound() throws Exception {
        org.mockito.Mockito.doThrow(UserCompanyException.notFound())
                .when(companyUserManagementService).removeUserFromCompany(companyId, "kc-remove");

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(delete(ControllerMockMvcSupport.COMPANIES + "/" + companyId + "/users/kc-remove")),
                404,
                "USER_COMPANY_NOT_FOUND");
    }
}
