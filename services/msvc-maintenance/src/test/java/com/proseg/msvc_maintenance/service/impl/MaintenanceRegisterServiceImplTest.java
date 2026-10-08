package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRegisterUpdateRequestDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRegisterResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceRegister;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.exception.MaintenanceRegisterException;
import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import com.proseg.msvc_maintenance.mapper.MaintenanceRegisterMapper;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.MaintenanceRegisterRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceRequestRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRegisterServiceImplTest {

    @Mock
    private MaintenanceRegisterRepository maintenanceRegisterRepository;
    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private MaintenanceRegisterMapper maintenanceRegisterMapper;
    @Mock
    private MaintenanceRequestMapper maintenanceRequestMapper;

    @InjectMocks
    private MaintenanceRegisterServiceImpl service;

    @Captor
    private ArgumentCaptor<MaintenanceRegister> registerCaptor;

    @Captor
    private ArgumentCaptor<List<String>> sortCaptor;

    private UUID registerId;
    private UUID requestId;
    private MaintenanceRequest maintenanceRequest;
    private MaintenanceRegister register;

    @BeforeEach
    void setUp() {
        registerId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        maintenanceRequest = MaintenanceRequest.builder()
                .id(requestId)
                .status(MaintenanceStatus.PENDING)
                .startDate(LocalDate.of(2026, 1, 10))
                .endDate(LocalDate.of(2026, 1, 12))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .campusId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .build();
        register = MaintenanceRegister.builder()
                .id(registerId)
                .maintenanceRequest(maintenanceRequest)
                .status(MaintenanceStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("findAssigned con status filtra por estado exacto")
    void findAssigned_cuandoStatusPresente_usaRepositorioConStatus() {
        String keycloakUserId = "kc-1";
        Pageable pageable = PageRequest.of(0, 10);
        Page<MaintenanceRequest> page = new PageImpl<>(List.of(maintenanceRequest));
        when(maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserIdAndStatus(
                keycloakUserId, MaintenanceStatus.COMPLETED, pageable)).thenReturn(page);

        service.findAssigned(keycloakUserId, MaintenanceStatus.COMPLETED, pageable);

        verify(maintenanceRequestRepository).findByAssignedTechnicians_KeycloakUserIdAndStatus(
                keycloakUserId, MaintenanceStatus.COMPLETED, pageable);
        verify(maintenanceRequestRepository, never())
                .findByAssignedTechnicians_KeycloakUserIdAndStatusIn(any(), any(), any());
    }

    @Test
    @DisplayName("findAssigned sin status usa PENDING por defecto")
    void findAssigned_cuandoStatusNull_usaPendingPorDefecto() {
        String keycloakUserId = "kc-1";
        Pageable pageable = PageRequest.of(0, 10);
        Page<MaintenanceRequest> page = new PageImpl<>(List.of(maintenanceRequest));
        when(maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserIdAndStatusIn(
                keycloakUserId, List.of(MaintenanceStatus.PENDING), pageable)).thenReturn(page);

        service.findAssigned(keycloakUserId, null, pageable);

        verify(maintenanceRequestRepository).findByAssignedTechnicians_KeycloakUserIdAndStatusIn(
                eq(keycloakUserId), eq(List.of(MaintenanceStatus.PENDING)), eq(pageable));
    }

    @Test
    @DisplayName("findHistory sin alcance de empresa filtra por técnico")
    void findHistory_cuandoCompanyWideFalse_usaAsignacionDelTecnico() {
        String keycloakUserId = "kc-1";
        Pageable pageable = PageRequest.of(0, 10);
        when(maintenanceRequestRepository.findByAssignedTechnicians_KeycloakUserId(keycloakUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(maintenanceRequest)));

        service.findHistory(keycloakUserId, false, pageable);

        verify(maintenanceRequestRepository).findByAssignedTechnicians_KeycloakUserId(keycloakUserId, pageable);
        verifyNoInteractions(userCompanyRepository);
    }

    @Test
    @DisplayName("findHistory companyWide consulta por empresas del usuario")
    void findHistory_cuandoCompanyWideTrue_usaIdsDeEmpresa() {
        String keycloakUserId = "kc-1";
        Pageable pageable = PageRequest.of(0, 10);
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        when(userCompanyRepository.findAllByKeycloakUserId(keycloakUserId)).thenReturn(List.of(
                UserCompany.builder().company(Company.builder().id(companyA).build()).build(),
                UserCompany.builder().company(Company.builder().id(companyB).build()).build()));
        when(maintenanceRequestRepository.findByCompany_IdIn(List.of(companyA, companyB), pageable))
                .thenReturn(new PageImpl<>(List.of(maintenanceRequest)));

        service.findHistory(keycloakUserId, true, pageable);

        verify(maintenanceRequestRepository).findByCompany_IdIn(List.of(companyA, companyB), pageable);
    }

    @Test
    @DisplayName("findHistory companyWide sin empresas devuelve página vacía")
    void findHistory_cuandoSinEmpresas_noConsultaSolicitudes() {
        String keycloakUserId = "kc-1";
        Pageable pageable = PageRequest.of(0, 10);
        when(userCompanyRepository.findAllByKeycloakUserId(keycloakUserId)).thenReturn(List.of());

        Page<MaintenanceRequestResponseDto> result = service.findHistory(keycloakUserId, true, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(maintenanceRequestRepository, never()).findByCompany_IdIn(any(), any());
    }

    @Test
    @DisplayName("getOrCreateByRequest sin solicitud lanza notFound")
    void getOrCreateByRequest_cuandoSolicitudNoExiste_lanzaNotFound() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreateByRequest(requestId))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.notFound().getErrorCode()));
    }

    @Test
    @DisplayName("getOrCreateByRequest reutiliza register existente")
    void getOrCreateByRequest_cuandoRegisterExiste_noGuarda() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(maintenanceRequest));
        when(maintenanceRegisterRepository.findByMaintenanceRequestId(requestId)).thenReturn(Optional.of(register));
        when(maintenanceRegisterMapper.toResponse(register)).thenReturn(MaintenanceRegisterResponseDto.builder().build());

        service.getOrCreateByRequest(requestId);

        verify(maintenanceRegisterRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateByRequest crea register pendiente copiando fechas de la solicitud")
    void getOrCreateByRequest_cuandoNoExiste_creaRegisterPendiente() {
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(maintenanceRequest));
        when(maintenanceRegisterRepository.findByMaintenanceRequestId(requestId)).thenReturn(Optional.empty());
        when(maintenanceRegisterRepository.save(any(MaintenanceRegister.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRegisterMapper.toResponse(any(MaintenanceRegister.class)))
                .thenReturn(MaintenanceRegisterResponseDto.builder().build());

        service.getOrCreateByRequest(requestId);

        verify(maintenanceRegisterRepository).save(registerCaptor.capture());
        MaintenanceRegister created = registerCaptor.getValue();
        assertThat(created.getMaintenanceRequest()).isSameAs(maintenanceRequest);
        assertThat(created.getStatus()).isEqualTo(MaintenanceStatus.PENDING);
        assertThat(created.getStartDate()).isEqualTo(maintenanceRequest.getStartDate());
        assertThat(created.getEndDate()).isEqualTo(maintenanceRequest.getEndDate());
        assertThat(created.getStartTime()).isEqualTo(maintenanceRequest.getStartTime());
        assertThat(created.getEndTime()).isEqualTo(maintenanceRequest.getEndTime());
    }

    @Test
    @DisplayName("update copia fechas y horas al register")
    void update_cuandoExiste_actualizaFechasYHoras() {
        MaintenanceRegisterUpdateRequestDto dto = MaintenanceRegisterUpdateRequestDto.builder()
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 2))
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(18, 45))
                .build();
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(maintenanceRegisterRepository.save(any(MaintenanceRegister.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRegisterMapper.toResponse(any(MaintenanceRegister.class)))
                .thenReturn(MaintenanceRegisterResponseDto.builder().build());

        service.update(registerId, dto);

        verify(maintenanceRegisterRepository).save(registerCaptor.capture());
        MaintenanceRegister saved = registerCaptor.getValue();
        assertThat(saved.getStartDate()).isEqualTo(dto.getStartDate());
        assertThat(saved.getEndDate()).isEqualTo(dto.getEndDate());
        assertThat(saved.getStartTime()).isEqualTo(dto.getStartTime());
        assertThat(saved.getEndTime()).isEqualTo(dto.getEndTime());
    }

    @Test
    @DisplayName("update sin register lanza notFound")
    void update_cuandoNoExiste_lanzaNotFound() {
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(registerId, MaintenanceRegisterUpdateRequestDto.builder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .startTime(LocalTime.now())
                .endTime(LocalTime.now())
                .build()))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.notFound().getErrorCode()));
    }

    @Test
    @DisplayName("finalizeRegister con solicitud PENDING completa register y solicitud")
    void finalizeRegister_cuandoPending_marcaCompletado() {
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(maintenanceRegisterRepository.save(any(MaintenanceRegister.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRegisterMapper.toResponse(any(MaintenanceRegister.class)))
                .thenReturn(MaintenanceRegisterResponseDto.builder().build());

        service.finalizeRegister(registerId);

        verify(maintenanceRegisterRepository).save(registerCaptor.capture());
        MaintenanceRegister saved = registerCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(saved.getMaintenanceRequest().getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    @DisplayName("finalizeRegister con solicitud CANCELLED lanza transición inválida")
    void finalizeRegister_cuandoCancelada_lanzaInvalidStatusTransition() {
        maintenanceRequest.setStatus(MaintenanceStatus.CANCELLED);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        assertThatThrownBy(() -> service.finalizeRegister(registerId))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.invalidStatusTransition(
                                MaintenanceStatus.CANCELLED, MaintenanceStatus.COMPLETED).getErrorCode()));

        verify(maintenanceRegisterRepository, never()).save(any());
    }

    @Test
    @DisplayName("finalizeRegister con solicitud ya COMPLETED vuelve a guardar (mismo estado permitido)")
    void finalizeRegister_cuandoSolicitudYaCompletada_guardaDeNuevo() {
        // Comportamiento real: validateOrThrow(COMPLETED, COMPLETED) permite la transición (mismo estado).
        maintenanceRequest.setStatus(MaintenanceStatus.COMPLETED);
        register.setStatus(MaintenanceStatus.COMPLETED);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(maintenanceRegisterRepository.save(any(MaintenanceRegister.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRegisterMapper.toResponse(any(MaintenanceRegister.class)))
                .thenReturn(MaintenanceRegisterResponseDto.builder().build());

        service.finalizeRegister(registerId);

        verify(maintenanceRegisterRepository).save(registerCaptor.capture());
        MaintenanceRegister saved = registerCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(saved.getMaintenanceRequest().getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    }

    @Test
    @DisplayName("finalizeRegister sin register lanza notFound")
    void finalizeRegister_cuandoNoExiste_lanzaNotFound() {
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalizeRegister(registerId))
                .isInstanceOf(MaintenanceRegisterException.class);
    }

    @Test
    @DisplayName("findRegisterAssets con buildingId consulta inventario por edificio")
    void findRegisterAssets_cuandoHayBuilding_usaFindAssetsByBuilding() {
        UUID buildingId = maintenanceRequest.getBuildingId();
        Pageable pageable = PageRequest.of(1, 20, Sort.by(
                Sort.Order.desc("propiedad"),
                Sort.Order.asc("direccion")));
        PageResponse<InventoryAssetResponseDto> data = PageResponse.<InventoryAssetResponseDto>builder()
                .content(List.of(InventoryAssetResponseDto.builder().id(UUID.randomUUID()).build()))
                .totalElements(1)
                .build();
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(inventoryClient.findAssetsByBuilding(
                eq(buildingId), eq("buscar"), eq(1), eq(20), sortCaptor.capture(), eq(Map.of("k", "v"))))
                .thenReturn(new ApiResponse<>(null, data, 200));

        service.findRegisterAssets(registerId, "buscar", Map.of("k", "v"), pageable);

        assertThat(sortCaptor.getValue()).containsExactly("propiedad,desc", "direccion,asc");
        verify(inventoryClient, never()).findAssetsByCampus(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("findRegisterAssets sin buildingId consulta inventario por campus")
    void findRegisterAssets_cuandoSinBuilding_usaFindAssetsByCampus() {
        maintenanceRequest.setBuildingId(null);
        UUID campusId = maintenanceRequest.getCampusId();
        Pageable pageable = PageRequest.of(0, 10);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(inventoryClient.findAssetsByCampus(
                eq(campusId), eq(null), eq(0), eq(10), eq(List.of()), eq(Map.of())))
                .thenReturn(new ApiResponse<>(null, PageResponse.<InventoryAssetResponseDto>builder()
                        .content(List.of())
                        .totalElements(0)
                        .build(), 200));

        Page<?> result = service.findRegisterAssets(registerId, null, Map.of(), pageable);

        assertThat(result.getContent()).isEmpty();
        verify(inventoryClient, never()).findAssetsByBuilding(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("findRegisterAssets con respuesta nula o sin contenido devuelve página vacía")
    void findRegisterAssets_cuandoRespuestaInvalida_devuelvePaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        when(inventoryClient.findAssetsByBuilding(any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(null);
        assertThat(service.findRegisterAssets(registerId, null, Map.of(), pageable).getTotalElements()).isZero();

        when(inventoryClient.findAssetsByBuilding(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new ApiResponse<>(null, null, 200));
        assertThat(service.findRegisterAssets(registerId, null, Map.of(), pageable).getTotalElements()).isZero();

        when(inventoryClient.findAssetsByBuilding(any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new ApiResponse<>(null, PageResponse.<InventoryAssetResponseDto>builder()
                        .content(null)
                        .totalElements(5)
                        .build(), 200));
        assertThat(service.findRegisterAssets(registerId, null, Map.of(), pageable).getContent()).isEmpty();
    }
}
