package com.proseg.msvc_maintenance.service.impl;

import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceEmail;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent;
import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceImplUpdateTest {

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
    private ArgumentCaptor<MaintenanceRequest> savedCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private UUID requestId;
    private UUID companyId;
    private UUID campusId;
    private UUID buildingId;
    private UUID technicianId;
    private Company company;
    private MaintenanceRequest existing;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        campusId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        technicianId = UUID.randomUUID();
        company = Company.builder().id(companyId).name("Empresa").legalId("3-101").build();
        existing = MaintenanceRequest.builder()
                .id(requestId)
                .company(company)
                .description("desc original")
                .status(MaintenanceStatus.PENDING)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .campusId(campusId)
                .buildingId(buildingId)
                .assignedTechnicians(List.of(UserCompany.builder()
                        .id(technicianId)
                        .company(company)
                        .keycloakUserId("kc-old")
                        .build()))
                .emails(List.of(MaintenanceEmail.builder().email("a@b.com").build()))
                .build();
    }

    @Test
    @DisplayName("update con solicitud inexistente lanza notFound")
    void update_cuandoSolicitudNoExiste_lanzaNotFound() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(requestId, baseUpdateDto()))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.notFound().getErrorCode()));

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("update con transición COMPLETED a PENDING lanza invalidStatusTransition")
    void update_cuandoTransicionCompletedAPending_lanzaInvalidStatusTransition() {
        existing.setStatus(MaintenanceStatus.COMPLETED);
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        MaintenanceRequestRequestDto dto = baseUpdateDto();
        dto.setStatus(MaintenanceStatus.PENDING);

        assertThatThrownBy(() -> service.update(requestId, dto))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.invalidStatusTransition(
                                MaintenanceStatus.COMPLETED, MaintenanceStatus.PENDING).getErrorCode()));

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("update con transición CANCELLED a COMPLETED lanza invalidStatusTransition")
    void update_cuandoTransicionCancelledACompleted_lanzaInvalidStatusTransition() {
        existing.setStatus(MaintenanceStatus.CANCELLED);
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        MaintenanceRequestRequestDto dto = baseUpdateDto();
        dto.setStatus(MaintenanceStatus.COMPLETED);

        assertThatThrownBy(() -> service.update(requestId, dto))
                .isInstanceOf(MaintenanceRequestException.class);

        verify(maintenanceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("update PENDING a COMPLETED no sincroniza el registro de mantenimiento")
    void update_cuandoPasaACompleted_noInvocaSyncRegisterStatus() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        stubSaveAndMapperFromDto();
        MaintenanceRequestServiceImplTestSupport.stubUpdateDependencies(
                companyRepository, userCompanyRepository, maintenanceEmailRepository,
                inventoryClient, maintenanceRequestMapper, companyId, campusId, technicianId);
        MaintenanceRequestServiceImplTestSupport.stubCampusName(inventoryClient, campusId, "Campus");
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, buildingId, "Edificio");

        MaintenanceRequestRequestDto dto = baseUpdateDto();
        dto.setStatus(MaintenanceStatus.COMPLETED);

        service.update(requestId, dto);

        verify(maintenanceRegisterRepository, never()).findByMaintenanceRequestId(any());
        MaintenanceRequestNotificationDomainEvent event = captureSingleNotification();
        assertThat(event.actionType()).isEqualTo("UPDATED");
        assertThat(event.changedFields()).anyMatch(field -> field.contains("Estado"));
    }

    @Test
    @DisplayName("update con cambios publica UPDATED y lista los campos modificados")
    void update_cuandoCambianCampos_publicaChangedFields() {
        UUID newCampusId = UUID.randomUUID();
        UUID newBuildingId = UUID.randomUUID();
        UUID newTechnicianId = UUID.randomUUID();
        UserCompany newTechnician = UserCompany.builder()
                .id(newTechnicianId)
                .company(company)
                .keycloakUserId("kc-new")
                .userEmail("resp@empresa.com")
                .build();

        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        stubSaveAndMapperFromDto();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllById(List.of(newTechnicianId))).thenReturn(List.of(newTechnician));
        MaintenanceRequestServiceImplTestSupport.stubCampusEmails(inventoryClient, newCampusId, List.of("b@c.com"));
        when(inventoryClient.findBuildingEmails(newBuildingId))
                .thenReturn(new com.proseg.common.api.response.ApiResponse<>(null, List.of(), 200));
        when(maintenanceEmailRepository.findByEmail("b@c.com"))
                .thenReturn(Optional.of(MaintenanceEmail.builder().email("b@c.com").build()));
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(
                com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto.builder().build());

        MaintenanceRequestServiceImplTestSupport.stubCampusName(inventoryClient, campusId, "Campus Viejo");
        MaintenanceRequestServiceImplTestSupport.stubCampusName(inventoryClient, newCampusId, "Campus Nuevo");
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, buildingId, "Edificio Viejo");
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, newBuildingId, "Edificio Nuevo");

        MaintenanceRequestRequestDto dto = MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .description("desc nueva")
                .status(MaintenanceStatus.COMPLETED)
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 2))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .campusId(newCampusId.toString())
                .buildingId(newBuildingId)
                .assignedTechnicianIds(List.of(newTechnicianId))
                .responsibleUserCompanyId(newTechnicianId)
                .emails(List.of("b@c.com"))
                .build();

        service.update(requestId, dto);

        MaintenanceRequestNotificationDomainEvent event = captureSingleNotification();
        assertThat(event.actionType()).isEqualTo("UPDATED");
        List<String> changes = event.changedFields();
        assertThat(changes).anyMatch(c -> c.startsWith("Descripcion:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Estado:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Fecha inicio:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Fecha fin:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Hora inicio:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Hora fin:"));
        assertThat(changes).anyMatch(c -> c.startsWith("Campus:") && c.contains("Campus Viejo") && c.contains("Campus Nuevo"));
        assertThat(changes).anyMatch(c -> c.startsWith("Edificio:"));
        assertThat(changes).contains("Tecnicos asignados actualizados");
        assertThat(changes).anyMatch(c -> c.startsWith("Responsable:"));
        assertThat(changes).contains("Correos asociados actualizados");
    }

    @Test
    @DisplayName("update sin cambios igual publica UPDATED con changedFields vacío")
    void update_cuandoSinCambios_publicaUpdatedConListaVacia() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(maintenanceRequestMapper).updateEntityFromRequest(any(), any());
        MaintenanceRequestServiceImplTestSupport.stubUpdateDependencies(
                companyRepository, userCompanyRepository, maintenanceEmailRepository,
                inventoryClient, maintenanceRequestMapper, companyId, campusId, technicianId);
        MaintenanceRequestServiceImplTestSupport.stubCampusName(inventoryClient, campusId, "Campus");
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, buildingId, "Edificio");

        service.update(requestId, baseUpdateDto());

        MaintenanceRequestNotificationDomainEvent event = captureSingleNotification();
        assertThat(event.actionType()).isEqualTo("UPDATED");
        assertThat(event.changedFields()).isEmpty();
    }

    @Test
    @DisplayName("update con estado final distinto de CANCELLED limpia cancellationReason")
    void update_cuandoEstadoNoCancelled_limpiaCancellationReason() {
        existing.setStatus(MaintenanceStatus.PENDING);
        existing.setCancellationReason("motivo previo");
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        stubSaveAndMapperFromDto();
        MaintenanceRequestServiceImplTestSupport.stubUpdateDependencies(
                companyRepository, userCompanyRepository, maintenanceEmailRepository,
                inventoryClient, maintenanceRequestMapper, companyId, campusId, technicianId);
        MaintenanceRequestServiceImplTestSupport.stubCampusName(inventoryClient, campusId, "Campus");
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, buildingId, "Edificio");

        MaintenanceRequestRequestDto dto = baseUpdateDto();
        dto.setStatus(MaintenanceStatus.PENDING);

        service.update(requestId, dto);

        verify(maintenanceRequestRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCancellationReason()).isNull();
    }

    @Test
    @DisplayName("update tolera fallo de inventory al resolver nombres de campus")
    void update_cuandoInventoryFallaAlResolverCampus_noRompeActualizacion() {
        UUID newCampusId = UUID.randomUUID();
        existing.setCampusId(campusId);
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        stubSaveAndMapperFromDto();
        MaintenanceRequestServiceImplTestSupport.stubUpdateDependencies(
                companyRepository, userCompanyRepository, maintenanceEmailRepository,
                inventoryClient, maintenanceRequestMapper, companyId, newCampusId, technicianId);
        when(inventoryClient.findCampusById(campusId)).thenThrow(new RuntimeException("inventory down"));
        when(inventoryClient.findCampusById(newCampusId)).thenThrow(new RuntimeException("inventory down"));
        MaintenanceRequestServiceImplTestSupport.stubBuildingName(inventoryClient, buildingId, "Edificio");

        MaintenanceRequestRequestDto dto = baseUpdateDto();
        dto.setCampusId(newCampusId.toString());

        service.update(requestId, dto);

        MaintenanceRequestNotificationDomainEvent event = captureSingleNotification();
        assertThat(event.actionType()).isEqualTo("UPDATED");
        assertThat(event.changedFields()).anyMatch(c -> c.contains("Recinto no disponible"));
    }

    private MaintenanceRequestRequestDto baseUpdateDto() {
        return MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .description(existing.getDescription())
                .status(existing.getStatus())
                .startDate(existing.getStartDate())
                .endDate(existing.getEndDate())
                .startTime(existing.getStartTime())
                .endTime(existing.getEndTime())
                .campusId(campusId.toString())
                .buildingId(buildingId)
                .assignedTechnicianIds(List.of(technicianId))
                .emails(List.of("a@b.com"))
                .build();
    }

    private void stubSaveAndMapperFromDto() {
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            MaintenanceRequestRequestDto dto = inv.getArgument(0);
            MaintenanceRequest entity = inv.getArgument(1);
            entity.setDescription(dto.getDescription());
            if (dto.getStatus() != null) {
                entity.setStatus(dto.getStatus());
            }
            entity.setStartDate(dto.getStartDate());
            entity.setEndDate(dto.getEndDate());
            entity.setStartTime(dto.getStartTime());
            entity.setEndTime(dto.getEndTime());
            return null;
        }).when(maintenanceRequestMapper).updateEntityFromRequest(any(), any());
    }

    private MaintenanceRequestNotificationDomainEvent captureSingleNotification() {
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(MaintenanceRequestNotificationDomainEvent.class);
        return (MaintenanceRequestNotificationDomainEvent) eventCaptor.getValue();
    }
}
