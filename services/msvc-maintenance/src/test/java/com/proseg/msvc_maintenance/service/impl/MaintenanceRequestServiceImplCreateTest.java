package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceEmail;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.event.MaintenanceRequestCreatedDomainEvent;
import com.proseg.msvc_maintenance.exception.CompanyException;
import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.CompanyRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceEmailRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceRegisterRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import com.proseg.msvc_maintenance.security.Privileges;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceImplCreateTest {

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock
    private MaintenanceRegisterRepository maintenanceRegisterRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private MaintenanceEmailRepository maintenanceEmailRepository;
    @Mock
    private MaintenanceRequestMapper maintenanceRequestMapper;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MaintenanceRequestServiceImpl service;

    @Captor
    private ArgumentCaptor<MaintenanceRequest> savedRequestCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;
    @Captor
    private ArgumentCaptor<Specification<MaintenanceRequest>> specificationCaptor;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor
    private ArgumentCaptor<MaintenanceEmail> maintenanceEmailCaptor;

    private UUID companyId;
    private UUID campusId;
    private UUID buildingId;
    private UUID technicianId;
    private UUID responsibleId;
    private Company company;
    private UserCompany technician;
    private static final String KEYCLOAK_USER = "kc-requester";

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        campusId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        technicianId = UUID.randomUUID();
        responsibleId = technicianId;
        company = Company.builder()
                .id(companyId)
                .name("Empresa SA")
                .legalId("3-101-000")
                .build();
        technician = UserCompany.builder()
                .id(technicianId)
                .keycloakUserId("kc-tech-1")
                .company(company)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("create sin autenticación permite cualquier empresa")
    void create_cuandoSinAutenticacion_permiteCualquierEmpresa() {
        stubCreateHappyPath();
        when(maintenanceEmailRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));
        SecurityContextHolder.clearContext();

        service.create(validCreateDto());

        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getCompany().getId()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("create con privilegio seleccionar empresa permite cualquier empresa")
    void create_cuandoTienePrivilegioSeleccionarEmpresa_permiteCualquierEmpresa() {
        setAuthentication(jwtPrincipal(KEYCLOAK_USER), List.of(
                new SimpleGrantedAuthority(Privileges.SolicitudesMantenimiento.SELECCIONAR_EMPRESA)));

        UUID otraEmpresa = UUID.randomUUID();
        Company otraCompany = Company.builder().id(otraEmpresa).name("Otra").build();
        UserCompany tecnicoOtraEmpresa = UserCompany.builder()
                .id(technicianId)
                .keycloakUserId("kc-tech-1")
                .company(otraCompany)
                .build();
        when(companyRepository.findById(otraEmpresa)).thenReturn(Optional.of(otraCompany));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(tecnicoOtraEmpresa));
        when(maintenanceRequestMapper.toEntity(any(MaintenanceRequestRequestDto.class))).thenReturn(new MaintenanceRequest());
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any(MaintenanceRequest.class)))
                .thenReturn(MaintenanceRequestResponseDto.builder().build());
        stubCampusEmails(List.of("a@b.com"));
        when(maintenanceEmailRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setCompanyId(otraEmpresa.toString());

        service.create(dto);

        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getCompany().getId()).isEqualTo(otraEmpresa);
        verify(userCompanyRepository, never()).findAllByKeycloakUserId(any());
    }

    @Test
    @DisplayName("create con Jwt de otra empresa lanza companyNotAllowed")
    void create_cuandoJwtEmpresaDistinta_lanzaCompanyNotAllowed() {
        setAuthentication(jwtPrincipal(KEYCLOAK_USER), List.of());
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER)).thenReturn(List.of(
                UserCompany.builder().company(Company.builder().id(UUID.randomUUID()).build()).build()));

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.companyNotAllowed().getErrorCode()));

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con Jwt de la misma empresa se permite")
    void create_cuandoJwtMismaEmpresa_permiteCrear() {
        stubCreateHappyPath();
        when(maintenanceEmailRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));
        setAuthentication(jwtPrincipal(KEYCLOAK_USER), List.of());
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER)).thenReturn(List.of(
                UserCompany.builder().company(company).build()));

        service.create(validCreateDto());

        verify(maintenanceRequestRepository).save(any(MaintenanceRequest.class));
    }

    @Test
    @DisplayName("create con Jwt sin empresa asociada lanza noAssociatedCompany")
    void create_cuandoJwtSinEmpresa_lanzaNoAssociatedCompany() {
        setAuthentication(jwtPrincipal(KEYCLOAK_USER), List.of());
        when(userCompanyRepository.findAllByKeycloakUserId(KEYCLOAK_USER)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode())
                        .isEqualTo(CompanyException.noAssociatedCompany().getErrorCode()));
    }

    @Test
    @DisplayName("create con principal que no es Jwt lanza companyNotAllowed")
    void create_cuandoPrincipalNoEsJwt_lanzaCompanyNotAllowed() {
        setAuthentication(new UsernamePasswordAuthenticationToken("no-jwt", null, List.of()), List.of());

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.companyNotAllowed().getErrorCode()));
    }

    @Test
    @DisplayName("create camino feliz persiste solicitud pendiente y publica evento")
    void create_cuandoDatosValidos_guardaSolicitudYPublicaEvento() {
        stubCreateHappyPath();
        when(maintenanceEmailRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));
        MaintenanceRequestRequestDto dto = validCreateDto();

        service.create(dto);

        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        MaintenanceRequest saved = savedRequestCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.PENDING);
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getCampusId()).isEqualTo(campusId);
        assertThat(saved.getBuildingId()).isEqualTo(buildingId);
        assertThat(saved.getAssignedTechnicians()).containsExactly(technician);
        assertThat(saved.getResponsibleUserCompany()).isSameAs(technician);
        assertThat(saved.getEmails()).extracting(MaintenanceEmail::getEmail).containsExactly("a@b.com");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(MaintenanceRequestCreatedDomainEvent.class);
        MaintenanceRequestCreatedDomainEvent event = (MaintenanceRequestCreatedDomainEvent) eventCaptor.getValue();
        assertThat(event.requestId()).isEqualTo(saved.getId());
        assertThat(event.emails()).containsExactly("a@b.com");
        assertThat(event.companyName()).isEqualTo("Empresa SA");
        assertThat(event.technicianKeycloakIds()).containsExactly("kc-tech-1");
        assertThat(event.responsibleKeycloakId()).isEqualTo("kc-tech-1");
    }

    @Test
    @DisplayName("create con companyId inválido lanza IllegalArgumentException")
    void create_cuandoCompanyIdInvalido_lanzaIllegalArgumentException() {
        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setCompanyId("no-uuid");

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El campo 'companyId' debe ser un UUID válido");
    }

    @Test
    @DisplayName("create con campusId inválido lanza IllegalArgumentException")
    void create_cuandoCampusIdInvalido_lanzaIllegalArgumentException() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setCampusId("no-uuid");

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El campo 'campusId' debe ser un UUID válido");
    }

    @Test
    @DisplayName("create con empresa inexistente lanza CompanyException.notFound")
    void create_cuandoEmpresaNoExiste_lanzaCompanyNotFound() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode())
                        .isEqualTo(CompanyException.notFound().getErrorCode()));

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con técnico de otra empresa lanza IllegalArgumentException")
    void create_cuandoTecnicoOtraEmpresa_lanzaIllegalArgumentException() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(
                UserCompany.builder().id(technicianId).company(Company.builder().id(UUID.randomUUID()).build()).build()));

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Todos los técnicos asignados deben pertenecer a la empresa de la solicitud");
    }

    @Test
    @DisplayName("create con responsable fuera de técnicos lanza IllegalArgumentException")
    void create_cuandoResponsableNoAsignado_lanzaIllegalArgumentException() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setResponsibleUserCompanyId(UUID.randomUUID());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El técnico responsable debe estar en la lista de técnicos asignados");
    }

    @Test
    @DisplayName("create sin técnicos asignados deja lista vacía")
    void create_cuandoSinTecnicos_listaVacia() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(MaintenanceRequestResponseDto.builder().build());
        stubCampusEmails(List.of("a@b.com"));
        when(maintenanceEmailRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("a@b.com").build()));

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setAssignedTechnicianIds(null);
        dto.setResponsibleUserCompanyId(null);

        service.create(dto);

        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getAssignedTechnicians()).isEmpty();
        assertThat(savedRequestCaptor.getValue().getResponsibleUserCompany()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("create con correos null o vacíos no asocia emails")
    void create_cuandoCorreosNullOVacios_noAsociaEmails(List<String> emails) {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(MaintenanceRequestResponseDto.builder().build());

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setEmails(emails);

        service.create(dto);

        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getEmails()).isEmpty();
        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("create normaliza correo registrado sin distinguir mayúsculas")
    void create_cuandoCorreoRegistradoConOtraCapitalizacion_usaFormaRegistrada() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(MaintenanceRequestResponseDto.builder().build());
        stubCampusEmails(List.of("Registrado@Mail.com"));
        when(maintenanceEmailRepository.findByEmail("Registrado@Mail.com"))
                .thenReturn(Optional.empty());
        when(maintenanceEmailRepository.save(any(MaintenanceEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setEmails(List.of("registrado@mail.com"));

        service.create(dto);

        verify(maintenanceEmailRepository).save(maintenanceEmailCaptor.capture());
        assertThat(maintenanceEmailCaptor.getValue().getEmail()).isEqualTo("Registrado@Mail.com");
        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getEmails())
                .extracting(MaintenanceEmail::getEmail)
                .containsExactly("Registrado@Mail.com");
    }

    @Test
    @DisplayName("create con correos duplicados los deduplica")
    void create_cuandoCorreosDuplicados_deduplica() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(MaintenanceRequestResponseDto.builder().build());
        stubCampusEmails(List.of("a@b.com", "b@c.com"));
        when(maintenanceEmailRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(maintenanceEmailRepository.save(any(MaintenanceEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setEmails(List.of("a@b.com", "A@B.COM", "b@c.com"));

        service.create(dto);

        verify(maintenanceEmailRepository, times(2)).save(any(MaintenanceEmail.class));
        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getEmails()).hasSize(2);
    }

    @Test
    @DisplayName("create con correo no registrado lanza emailNotRegistered")
    void create_cuandoCorreoNoRegistrado_lanzaEmailNotRegistered() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        stubCampusEmails(List.of("a@b.com"));

        MaintenanceRequestRequestDto dto = validCreateDto();
        dto.setEmails(List.of("no@registrado.com"));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> {
                    MaintenanceRequestException mre = (MaintenanceRequestException) ex;
                    assertThat(mre.getErrorCode()).isEqualTo(
                            MaintenanceRequestException.emailNotRegistered("no@registrado.com").getErrorCode());
                    assertThat(mre.getMessage()).isEqualTo(
                            MaintenanceRequestException.emailNotRegistered("no@registrado.com").getMessage());
                });
    }

    @Test
    @DisplayName("create cuando inventory falla lanza registeredEmailsUnavailable")
    void create_cuandoInventoryFalla_lanzaRegisteredEmailsUnavailable() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any())).thenReturn(new MaintenanceRequest());
        when(inventoryClient.findCampusEmails(campusId)).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.create(validCreateDto()))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.registeredEmailsUnavailable().getErrorCode()));
    }

    @Test
    @DisplayName("create reutiliza correo existente en repositorio")
    void create_cuandoCorreoExisteEnRepo_noVuelveAGuardarEmail() {
        MaintenanceEmail existing = MaintenanceEmail.builder().email("a@b.com").build();
        stubCreateHappyPath();
        when(maintenanceEmailRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        service.create(validCreateDto());

        verify(maintenanceEmailRepository, never()).save(any());
        verify(maintenanceRequestRepository).save(savedRequestCaptor.capture());
        assertThat(savedRequestCaptor.getValue().getEmails()).containsExactly(existing);
    }

    @Test
    @DisplayName("create con correo nuevo lo persiste en repositorio de emails")
    void create_cuandoCorreoNuevo_guardaMaintenanceEmail() {
        stubCreateHappyPath();
        when(maintenanceEmailRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(maintenanceEmailRepository.save(any(MaintenanceEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(validCreateDto());

        verify(maintenanceEmailRepository).save(maintenanceEmailCaptor.capture());
        assertThat(maintenanceEmailCaptor.getValue().getEmail()).isEqualTo("a@b.com");
    }

    @Test
    @DisplayName("findById existente mapea respuesta")
    void findById_cuandoExiste_retornaDto() {
        UUID id = UUID.randomUUID();
        MaintenanceRequest entity = new MaintenanceRequest();
        MaintenanceRequestResponseDto dto = MaintenanceRequestResponseDto.builder().build();
        when(maintenanceRequestRepository.findById(id)).thenReturn(Optional.of(entity));
        when(maintenanceRequestMapper.toResponse(entity)).thenReturn(dto);

        assertThat(service.findById(id)).isSameAs(dto);
    }

    @Test
    @DisplayName("findById inexistente lanza notFound")
    void findById_cuandoNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(maintenanceRequestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.notFound().getErrorCode()));
    }

    @Test
    @DisplayName("findAll consulta repositorio con specification y pageable")
    void findAll_cuandoSeInvoca_usaSpecificationYPageable() {
        Pageable pageable = PageRequest.of(1, 5);
        when(maintenanceRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new MaintenanceRequest())));

        service.findAll("buscar", Map.of("status", "PENDING"), pageable);

        verify(maintenanceRequestRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByCompanyId con empresa inexistente lanza notFound")
    void findByCompanyId_cuandoEmpresaNoExiste_lanzaCompanyNotFound() {
        when(companyRepository.existsById(companyId)).thenReturn(false);

        assertThatThrownBy(() -> service.findByCompanyId(companyId, PageRequest.of(0, 10)))
                .isInstanceOf(CompanyException.class);

        verify(maintenanceRequestRepository, never()).findByCompanyId(any(), any());
    }

    @Test
    @DisplayName("findByCompanyId existente mapea página")
    void findByCompanyId_cuandoEmpresaExiste_mapeaPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(maintenanceRequestRepository.findByCompanyId(companyId, pageable))
                .thenReturn(new PageImpl<>(List.of(new MaintenanceRequest())));

        Page<MaintenanceRequestResponseDto> page = service.findByCompanyId(companyId, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(maintenanceRequestMapper).toResponse(any(MaintenanceRequest.class));
    }

    @Test
    @DisplayName("delete existente invoca delete del repositorio")
    void delete_cuandoExiste_eliminaSolicitud() {
        UUID id = UUID.randomUUID();
        MaintenanceRequest entity = new MaintenanceRequest();
        when(maintenanceRequestRepository.findById(id)).thenReturn(Optional.of(entity));

        service.delete(id);

        verify(maintenanceRequestRepository).delete(entity);
    }

    @Test
    @DisplayName("delete inexistente lanza notFound y no borra")
    void delete_cuandoNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(maintenanceRequestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(MaintenanceRequestException.class);

        verify(maintenanceRequestRepository, never()).delete(any(MaintenanceRequest.class));
    }

    private MaintenanceRequestRequestDto validCreateDto() {
        return MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .description("Mantenimiento programado")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .campusId(campusId.toString())
                .buildingId(buildingId)
                .assignedTechnicianIds(List.of(technicianId))
                .responsibleUserCompanyId(responsibleId)
                .emails(List.of("a@b.com"))
                .build();
    }

    private void stubCreateHappyPath() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(technicianId))).thenReturn(List.of(technician));
        when(maintenanceRequestMapper.toEntity(any(MaintenanceRequestRequestDto.class))).thenAnswer(inv -> {
            MaintenanceRequestRequestDto dto = inv.getArgument(0);
            MaintenanceRequest entity = new MaintenanceRequest();
            entity.setDescription(dto.getDescription());
            entity.setStartDate(dto.getStartDate());
            entity.setEndDate(dto.getEndDate());
            entity.setStartTime(dto.getStartTime());
            entity.setEndTime(dto.getEndTime());
            return entity;
        });
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
        when(maintenanceRequestMapper.toResponse(any(MaintenanceRequest.class)))
                .thenReturn(MaintenanceRequestResponseDto.builder().build());
        stubCampusEmails(List.of("a@b.com"));
    }

    private void stubCampusEmails(List<String> emails) {
        List<InventoryBuildingEmailResponseDto> dtoList = emails.stream()
                .map(email -> InventoryBuildingEmailResponseDto.builder().email(email).build())
                .toList();
        when(inventoryClient.findCampusEmails(campusId))
                .thenReturn(new ApiResponse<>(null, dtoList, 200));
        when(inventoryClient.findBuildingEmails(buildingId))
                .thenReturn(new ApiResponse<>(null, List.of(), 200));
    }

    private Jwt jwtPrincipal(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private void setAuthentication(Object principal, List<SimpleGrantedAuthority> authorities) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
