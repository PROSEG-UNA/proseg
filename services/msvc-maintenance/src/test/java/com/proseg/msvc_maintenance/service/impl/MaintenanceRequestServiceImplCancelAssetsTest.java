package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceRegister;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent;
import com.proseg.msvc_maintenance.exception.MaintenanceRequestException;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.*;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceImplCancelAssetsTest {

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
    private ArgumentCaptor<Object> eventCaptor;

    @Test
    @DisplayName("cancel sincroniza el registro asociado a CANCELLED")
    void cancel_cuandoPendiente_sincronizaRegister() {
        UUID requestId = UUID.randomUUID();
        MaintenanceRequest request = MaintenanceRequest.builder()
                .id(requestId)
                .status(MaintenanceStatus.PENDING)
                .company(Company.builder().name("Empresa").legalId("3-101").build())
                .build();
        MaintenanceRegister register = MaintenanceRegister.builder()
                .status(MaintenanceStatus.PENDING)
                .build();
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(maintenanceRegisterRepository.findByMaintenanceRequestId(requestId)).thenReturn(Optional.of(register));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(
                com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto.builder().build());

        service.cancel(requestId, "motivo");

        assertThat(register.getStatus()).isEqualTo(MaintenanceStatus.CANCELLED);
        verify(maintenanceRegisterRepository).findByMaintenanceRequestId(requestId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("cancel con motivo vacío normaliza a null")
    void cancel_cuandoMotivoVacio_normalizaMotivo(String reason) {
        UUID requestId = UUID.randomUUID();
        MaintenanceRequest request = MaintenanceRequest.builder()
                .id(requestId)
                .status(MaintenanceStatus.PENDING)
                .company(Company.builder().name("Empresa").legalId("3-101").build())
                .build();
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(
                com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto.builder().build());

        service.cancel(requestId, reason);

        verify(maintenanceRequestRepository).save(argThat(saved ->
                saved.getCancellationReason() == null));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MaintenanceRequestNotificationDomainEvent event = (MaintenanceRequestNotificationDomainEvent) eventCaptor.getValue();
        assertThat(event.cancellationReason()).isNull();
        assertThat(event.changedFields()).anyMatch(field -> field.contains("Motivo: Sin valor"));
    }

    @Test
    @DisplayName("cancel con solicitud ya CANCELLED permite la transición (mismo estado)")
    void cancel_cuandoYaCancelada_permiteRepetirCancelacion() {
        // Comportamiento real: validateOrThrow(CANCELLED, CANCELLED) no lanza (mismo estado).
        UUID requestId = UUID.randomUUID();
        MaintenanceRequest request = MaintenanceRequest.builder()
                .id(requestId)
                .status(MaintenanceStatus.CANCELLED)
                .company(Company.builder().name("Empresa").legalId("3-101").build())
                .build();
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(maintenanceRequestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRequestMapper.toResponse(any())).thenReturn(
                com.proseg.msvc_maintenance.dto.response.MaintenanceRequestResponseDto.builder().build());

        service.cancel(requestId, "motivo");

        verify(maintenanceRequestRepository).save(any(MaintenanceRequest.class));
        verify(eventPublisher).publishEvent(any(MaintenanceRequestNotificationDomainEvent.class));
    }

    @Test
    @DisplayName("cancel con solicitud completada lanza invalidStatusTransition")
    void cancel_cuandoCompletada_lanzaInvalidStatusTransition() {
        UUID requestId = UUID.randomUUID();
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(
                MaintenanceRequest.builder().id(requestId).status(MaintenanceStatus.COMPLETED).build()));

        assertThatThrownBy(() -> service.cancel(requestId, "motivo"))
                .isInstanceOf(MaintenanceRequestException.class);
    }

    @Test
    @DisplayName("cancel con solicitud inexistente lanza notFound")
    void cancel_cuandoNoExiste_lanzaNotFound() {
        UUID requestId = UUID.randomUUID();
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(requestId, "motivo"))
                .isInstanceOf(MaintenanceRequestException.class)
                .satisfies(ex -> assertThat(((MaintenanceRequestException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRequestException.notFound().getErrorCode()));
    }

    @Test
    @DisplayName("findAvailableAssets devuelve opciones mapeadas")
    void findAvailableAssets_cuandoHayDatos_mapeaContenido() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("assetNumber")));
        UUID assetId = UUID.randomUUID();
        PageResponse<InventoryAssetResponseDto> data = PageResponse.<InventoryAssetResponseDto>builder()
                .content(List.of(InventoryAssetResponseDto.builder().id(assetId).assetNumber("A-1").build()))
                .totalElements(1)
                .build();
        when(inventoryClient.findAssets(eq("filtro"), eq(0), eq(10), eq(List.of("assetNumber,asc"))))
                .thenReturn(new ApiResponse<>(null, data, 200));

        Page<?> page = service.findAvailableAssets("filtro", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAvailableAssets con respuesta inválida devuelve página vacía")
    void findAvailableAssets_cuandoRespuestaInvalida_devuelvePaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);

        when(inventoryClient.findAssets(isNull(), anyInt(), anyInt(), isNull())).thenReturn(null);
        assertThat(service.findAvailableAssets(null, pageable).getTotalElements()).isZero();

        when(inventoryClient.findAssets(isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(new ApiResponse<>(null, null, 200));
        assertThat(service.findAvailableAssets(null, pageable).getTotalElements()).isZero();

        when(inventoryClient.findAssets(isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(new ApiResponse<>(null, PageResponse.<InventoryAssetResponseDto>builder()
                        .content(null)
                        .totalElements(3)
                        .build(), 200));
        assertThat(service.findAvailableAssets(null, pageable).getContent()).isEmpty();
    }
}
