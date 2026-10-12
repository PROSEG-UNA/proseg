package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.dto.request.CompanyRequestDto;
import com.proseg.msvc_maintenance.dto.request.CreateManagedUserRequestDto;
import com.proseg.msvc_maintenance.dto.response.CompanyResponseDto;
import com.proseg.msvc_maintenance.dto.response.CreateManagedUserResponseDto;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.exception.CompanyException;
import com.proseg.msvc_maintenance.mapper.CompanyMapper;
import com.proseg.msvc_maintenance.repository.CompanyRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock
    private CompanyMapper companyMapper;
    @Mock
    private CompanyUserManagementService companyUserManagementService;
    @Mock
    private AuthClient authClient;

    @InjectMocks
    private CompanyServiceImpl service;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;
    @Captor
    private ArgumentCaptor<Specification<Company>> specificationCaptor;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private static final String KEYCLOAK_USER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    @Test
    @DisplayName("create camino feliz valida Keycloak, persiste empresa y sincroniza usuarios")
    void create_cuandoDatosValidos_persisteYSincronizaUsuarios() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .legalId("3-101-123456")
                .keycloakUserIds(List.of(KEYCLOAK_USER_ID))
                .build();
        Company entity = Company.builder().name("Acme SA").build();
        Company saved = Company.builder().id(UUID.randomUUID()).name("Acme SA").build();
        CompanyResponseDto responseDto = CompanyResponseDto.builder().name("Acme SA").build();
        KeycloakUserDto keycloakUser = new KeycloakUserDto();
        keycloakUser.setId(KEYCLOAK_USER_ID);
        ApiResponse<KeycloakUserDto> userResponse = new ApiResponse<>("ok", keycloakUser, 200);

        when(companyRepository.existsByLegalIdIgnoreCase("3-101-123456")).thenReturn(false);
        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(false);
        when(authClient.getUserById(KEYCLOAK_USER_ID)).thenReturn(userResponse);
        when(companyMapper.toEntity(request)).thenReturn(entity);
        when(companyRepository.save(any(Company.class))).thenReturn(saved);
        when(companyUserManagementService.syncUsers(saved, request.getKeycloakUserIds()))
                .thenReturn(List.of(UserCompany.builder().keycloakUserId(KEYCLOAK_USER_ID).build()));
        when(companyMapper.toResponse(saved)).thenReturn(responseDto);

        CompanyResponseDto result = service.create(request);

        assertThat(result).isSameAs(responseDto);
        verify(authClient).getUserById(KEYCLOAK_USER_ID);
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getLegalId()).isEqualTo("3-101-123456");
        verify(companyUserManagementService).syncUsers(saved, request.getKeycloakUserIds());
    }

    @Test
    @DisplayName("create normaliza cédula jurídica en blanco a null al guardar")
    void create_cuandoLegalIdEnBlanco_guardaLegalIdNull() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .legalId("   ")
                .build();
        Company entity = Company.builder().name("Acme SA").build();
        Company saved = Company.builder().id(UUID.randomUUID()).name("Acme SA").build();
        CompanyResponseDto responseDto = CompanyResponseDto.builder().build();

        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(false);
        when(companyMapper.toEntity(request)).thenReturn(entity);
        when(companyRepository.save(any(Company.class))).thenReturn(saved);
        when(companyUserManagementService.syncUsers(saved, null)).thenReturn(List.of());
        when(companyMapper.toResponse(saved)).thenReturn(responseDto);

        service.create(request);

        verify(companyRepository, never()).existsByLegalIdIgnoreCase(any());
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getLegalId()).isNull();
        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("create con nombre duplicado lanza duplicateName")
    void create_cuandoNombreDuplicado_lanzaDuplicateName() {
        CompanyRequestDto request = CompanyRequestDto.builder().name("Acme SA").build();
        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_DUPLICATE_NAME");
                    assertThat(ce.getMessage()).isEqualTo("Ya existe una empresa con el nombre: Acme SA");
                });

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con cédula jurídica duplicada lanza duplicateLegalId")
    void create_cuandoLegalIdDuplicado_lanzaDuplicateLegalId() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .legalId("3-101-999")
                .build();
        when(companyRepository.existsByLegalIdIgnoreCase("3-101-999")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_DUPLICATE_LEGAL_ID");
                    assertThat(ce.getMessage()).isEqualTo("Ya existe una empresa con la cédula jurídica: 3-101-999");
                });

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con usuario Keycloak inexistente (404) lanza invalidKeycloakUser")
    void create_cuandoKeycloakNotFound_lanzaInvalidKeycloakUser() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .keycloakUserIds(List.of(KEYCLOAK_USER_ID))
                .build();
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);

        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(false);
        when(authClient.getUserById(KEYCLOAK_USER_ID)).thenThrow(notFound);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("INVALID_KEYCLOAK_USER");
                    assertThat(ce.getMessage())
                            .isEqualTo("No existe un usuario en Keycloak con el id: " + KEYCLOAK_USER_ID);
                });

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("create cuando AuthClient responde sin datos lanza invalidKeycloakUser")
    void create_cuandoKeycloakSinDatos_lanzaInvalidKeycloakUser() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .keycloakUserIds(List.of(KEYCLOAK_USER_ID))
                .build();
        ApiResponse<KeycloakUserDto> emptyResponse = new ApiResponse<>("ok", null, 200);

        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(false);
        when(authClient.getUserById(KEYCLOAK_USER_ID)).thenReturn(emptyResponse);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("INVALID_KEYCLOAK_USER"));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("create cuando AuthClient falla con FeignException lanza userLookupFailed (no usa extractAuthMessage)")
    void create_cuandoAuthClientFeignError_lanzaUserLookupFailed() {
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .keycloakUserIds(List.of(KEYCLOAK_USER_ID))
                .build();
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(502);

        when(companyRepository.existsByNameIgnoreCase("Acme SA")).thenReturn(false);
        when(authClient.getUserById(KEYCLOAK_USER_ID)).thenThrow(feignEx);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("USER_LOOKUP_FAILED");
                    assertThat(ce.getMessage()).isEqualTo(
                            "No se pudo verificar el usuario contra el servicio de autenticacion. Intentalo de nuevo en unos minutos.");
                });

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("createManagedUser camino feliz devuelve datos del servicio de auth")
    void createManagedUser_cuandoAuthResponde_retornaDto() {
        CreateManagedUserRequestDto request = CreateManagedUserRequestDto.builder()
                .username("user1")
                .email("u@acme.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        CreateManagedUserResponseDto data = CreateManagedUserResponseDto.builder()
                .userId(KEYCLOAK_USER_ID)
                .build();
        ApiResponse<CreateManagedUserResponseDto> apiResponse = new ApiResponse<>("ok", data, 201);
        when(authClient.createManagedUser(request)).thenReturn(apiResponse);

        CreateManagedUserResponseDto result = service.createManagedUser(request);

        assertThat(result).isSameAs(data);
        verify(authClient).createManagedUser(request);
    }

    @Test
    @DisplayName("createManagedUser cuando AuthClient responde sin datos lanza inviteUserFailed")
    void createManagedUser_cuandoRespuestaSinDatos_lanzaInviteUserFailed() {
        CreateManagedUserRequestDto request = CreateManagedUserRequestDto.builder()
                .username("user1")
                .email("u@acme.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        when(authClient.createManagedUser(request)).thenReturn(new ApiResponse<>("ok", null, 200));

        assertThatThrownBy(() -> service.createManagedUser(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_USER_INVITATION_FAILED");
                    assertThat(ce.getMessage()).isEqualTo("No fue posible crear el usuario invitado");
                });
    }

    @Test
    @DisplayName("createManagedUser con FeignException y cuerpo JSON extrae message con extractAuthMessage")
    void createManagedUser_cuandoFeignConCuerpo_usaMensajeDelCuerpo() {
        CreateManagedUserRequestDto request = CreateManagedUserRequestDto.builder()
                .username("user1")
                .email("u@acme.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.contentUTF8()).thenReturn("{\"message\":\"Correo ya registrado\"}");
        when(authClient.createManagedUser(request)).thenThrow(feignEx);

        assertThatThrownBy(() -> service.createManagedUser(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_USER_INVITATION_FAILED");
                    assertThat(ce.getMessage()).isEqualTo("Correo ya registrado");
                });
    }

    @Test
    @DisplayName("createManagedUser con FeignException sin cuerpo usa mensaje por defecto de extractAuthMessage")
    void createManagedUser_cuandoFeignSinCuerpo_usaMensajePorDefecto() {
        CreateManagedUserRequestDto request = CreateManagedUserRequestDto.builder()
                .username("user1")
                .email("u@acme.com")
                .firstName("Ana")
                .lastName("Perez")
                .build();
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.contentUTF8()).thenReturn("");
        when(authClient.createManagedUser(request)).thenThrow(feignEx);

        assertThatThrownBy(() -> service.createManagedUser(request))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).isEqualTo("No fue posible crear el usuario invitado"));
    }

    @Test
    @DisplayName("findById existente mapea respuesta")
    void findById_cuandoExiste_retornaDto() {
        UUID id = UUID.randomUUID();
        Company company = Company.builder().id(id).name("Acme").build();
        CompanyResponseDto dto = CompanyResponseDto.builder().name("Acme").build();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(dto);

        assertThat(service.findById(id)).isSameAs(dto);
    }

    @Test
    @DisplayName("findById inexistente lanza notFound")
    void findById_cuandoNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_NOT_FOUND");
                    assertThat(ce.getMessage()).isEqualTo(
                            "No encontramos la empresa seleccionada. Verifica la información e inténtalo nuevamente.");
                });
    }

    @Test
    @DisplayName("findByKeycloakUserId con empresa asociada mapea respuesta")
    void findByKeycloakUserId_cuandoTieneEmpresa_retornaDto() {
        Company company = Company.builder().id(UUID.randomUUID()).name("Acme").build();
        UserCompany link = UserCompany.builder().keycloakUserId(KEYCLOAK_USER_ID).company(company).build();
        CompanyResponseDto dto = CompanyResponseDto.builder().name("Acme").build();
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(List.of(link));
        when(companyMapper.toResponse(company)).thenReturn(dto);

        assertThat(service.findByKeycloakUserId(KEYCLOAK_USER_ID)).isSameAs(dto);
    }

    @Test
    @DisplayName("findByKeycloakUserId sin empresa lanza noAssociatedCompany")
    void findByKeycloakUserId_cuandoSinEmpresa_lanzaNoAssociatedCompany() {
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findByKeycloakUserId(KEYCLOAK_USER_ID))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("COMPANY_NO_ASSOCIATED"));
    }

    @Test
    @DisplayName("hasCompany devuelve true cuando hay vínculos")
    void hasCompany_cuandoHayVinculos_retornaTrue() {
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER_ID))
                .thenReturn(List.of(UserCompany.builder().build()));

        assertThat(service.hasCompany(KEYCLOAK_USER_ID)).isTrue();
    }

    @Test
    @DisplayName("hasCompany devuelve false cuando no hay vínculos")
    void hasCompany_cuandoSinVinculos_retornaFalse() {
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(List.of());

        assertThat(service.hasCompany(KEYCLOAK_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("findAll consulta repositorio con specification y pageable")
    void findAll_cuandoSeInvoca_usaSpecificationYPageable() {
        Pageable pageable = PageRequest.of(1, 5);
        Company company = Company.builder().name("Acme").build();
        when(companyRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(company)));
        when(companyMapper.toResponse(company)).thenReturn(CompanyResponseDto.builder().build());

        Page<CompanyResponseDto> page = service.findAll("buscar", Map.of("name", "Acme"), pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(companyRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("update con empresa inexistente lanza notFound")
    void update_cuandoEmpresaNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, CompanyRequestDto.builder().name("X").build()))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("COMPANY_NOT_FOUND"));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("update camino feliz persiste y sincroniza usuarios")
    void update_cuandoEmpresaExiste_persisteYSincroniza() {
        UUID id = UUID.randomUUID();
        CompanyRequestDto request = CompanyRequestDto.builder()
                .name("Acme SA")
                .legalId("  ")
                .keycloakUserIds(List.of(KEYCLOAK_USER_ID))
                .build();
        Company existing = Company.builder().id(id).name("Antigua").build();
        CompanyResponseDto responseDto = CompanyResponseDto.builder().name("Acme SA").build();

        when(companyRepository.findById(id)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByNameIgnoreCaseAndIdNot("Acme SA", id)).thenReturn(false);
        when(companyRepository.save(existing)).thenReturn(existing);
        when(companyUserManagementService.syncUsers(existing, request.getKeycloakUserIds())).thenReturn(List.of());
        when(companyMapper.toResponse(existing)).thenReturn(responseDto);

        CompanyResponseDto result = service.update(id, request);

        assertThat(result).isSameAs(responseDto);
        verify(companyMapper).updateEntityFromRequest(request, existing);
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getLegalId()).isNull();
        verify(companyUserManagementService).syncUsers(existing, request.getKeycloakUserIds());
    }

    @Test
    @DisplayName("delete inexistente lanza notFound")
    void delete_cuandoNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("COMPANY_NOT_FOUND"));

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    @DisplayName("delete con solicitudes de mantenimiento asociadas lanza inUse y no borra")
    void delete_cuandoTieneSolicitudesMantenimiento_lanzaInUse() {
        UUID id = UUID.randomUUID();
        Company company = Company.builder().id(id).name("Acme SA").build();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(userCompanyRepository.existsByCompanyId(id)).thenReturn(false);
        when(maintenanceRequestRepository.existsByCompanyId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("COMPANY_IN_USE");
                    assertThat(ce.getMessage())
                            .isEqualTo("No se puede eliminar la empresa 'Acme SA' porque tiene registros relacionados");
                });

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    @DisplayName("delete con usuarios asociados lanza inUse (misma regla que solicitudes)")
    void delete_cuandoTieneUsuariosAsociados_lanzaInUse() {
        UUID id = UUID.randomUUID();
        Company company = Company.builder().id(id).name("Acme SA").build();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(userCompanyRepository.existsByCompanyId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("COMPANY_IN_USE"));

        verify(companyRepository, never()).delete(any(Company.class));
        verify(maintenanceRequestRepository, never()).existsByCompanyId(any());
    }

    @Test
    @DisplayName("delete sin registros relacionados elimina la empresa")
    void delete_cuandoSinRelaciones_eliminaEmpresa() {
        UUID id = UUID.randomUUID();
        Company company = Company.builder().id(id).name("Acme SA").build();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(userCompanyRepository.existsByCompanyId(id)).thenReturn(false);
        when(maintenanceRequestRepository.existsByCompanyId(id)).thenReturn(false);

        service.delete(id);

        verify(companyRepository).delete(company);
    }
}
