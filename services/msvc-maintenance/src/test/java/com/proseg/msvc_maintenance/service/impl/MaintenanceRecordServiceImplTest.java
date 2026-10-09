package com.proseg.msvc_maintenance.service.impl;

import com.proseg.msvc_maintenance.dto.request.MaintenanceRecordRequestDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceRecordResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceRecord;
import com.proseg.msvc_maintenance.entity.MaintenanceRegister;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.exception.MaintenanceRegisterException;
import com.proseg.msvc_maintenance.mapper.MaintenanceRecordMapper;
import com.proseg.msvc_maintenance.repository.MaintenanceRecordRepository;
import com.proseg.msvc_maintenance.repository.MaintenanceRegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordServiceImplTest {

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;
    @Mock
    private MaintenanceRegisterRepository maintenanceRegisterRepository;
    @Mock
    private MaintenanceRecordMapper maintenanceRecordMapper;

    @InjectMocks
    private MaintenanceRecordServiceImpl service;

    @Captor
    private ArgumentCaptor<MaintenanceRecord> recordCaptor;

    private UUID registerId;
    private UUID assetId;
    private MaintenanceRegister register;
    private MaintenanceRequest pendingRequest;
    private Company company;
    private static final String KEYCLOAK_USER = "kc-tech-1";
    private static final String USER_EMAIL = "tech@empresa.com";

    @BeforeEach
    void setUp() {
        registerId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        company = Company.builder().id(UUID.randomUUID()).name("Empresa").build();
        pendingRequest = MaintenanceRequest.builder()
                .id(UUID.randomUUID())
                .status(MaintenanceStatus.PENDING)
                .company(company)
                .assignedTechnicians(List.of(UserCompany.builder().keycloakUserId(KEYCLOAK_USER).build()))
                .build();
        register = MaintenanceRegister.builder()
                .id(registerId)
                .maintenanceRequest(pendingRequest)
                .build();
    }

    @Test
    @DisplayName("create persiste el registro con los datos esperados")
    void create_cuandoCaminoFeliz_guardaRegistroConDatosCorrectos() {
        MaintenanceRecordRequestDto requestDto = MaintenanceRecordRequestDto.builder()
                .assetId(assetId)
                .description("Trabajo realizado")
                .build();
        MaintenanceRecordResponseDto responseDto = MaintenanceRecordResponseDto.builder().build();

        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRecordMapper.toResponse(any(MaintenanceRecord.class))).thenReturn(responseDto);

        MaintenanceRecordResponseDto result = service.create(registerId, requestDto, KEYCLOAK_USER, USER_EMAIL);

        verify(maintenanceRecordRepository).save(recordCaptor.capture());
        MaintenanceRecord saved = recordCaptor.getValue();
        assertThat(saved.getMaintenanceRegister()).isSameAs(register);
        assertThat(saved.getAssetId()).isEqualTo(assetId);
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER);
        assertThat(saved.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(saved.getDescription()).isEqualTo("Trabajo realizado");
        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("create sin register lanza notFound y no guarda")
    void create_cuandoRegisterNoExiste_lanzaNotFound() {
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                registerId,
                MaintenanceRecordRequestDto.builder().assetId(assetId).description("x").build(),
                KEYCLOAK_USER,
                USER_EMAIL))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.notFound().getErrorCode()));

        verify(maintenanceRecordRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = MaintenanceStatus.class, names = {"COMPLETED", "CANCELLED"})
    @DisplayName("create con solicitud no pendiente lanza notPending")
    void create_cuandoSolicitudNoPendiente_lanzaNotPending(MaintenanceStatus status) {
        pendingRequest.setStatus(status);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        assertThatThrownBy(() -> service.create(
                registerId,
                MaintenanceRecordRequestDto.builder().assetId(assetId).description("x").build(),
                KEYCLOAK_USER,
                USER_EMAIL))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.notPending().getErrorCode()));

        verify(maintenanceRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("create sin técnicos asignados lanza userWithoutCompany")
    void create_cuandoListaTecnicosNull_lanzaUserWithoutCompany() {
        pendingRequest.setAssignedTechnicians(null);
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        assertThatThrownBy(() -> service.create(
                registerId,
                MaintenanceRecordRequestDto.builder().assetId(assetId).description("x").build(),
                KEYCLOAK_USER,
                USER_EMAIL))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.userWithoutCompany().getErrorCode()));

        verify(maintenanceRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con técnico distinto lanza userWithoutCompany")
    void create_cuandoTecnicoNoAsignado_lanzaUserWithoutCompany() {
        pendingRequest.setAssignedTechnicians(List.of(
                UserCompany.builder().keycloakUserId("otro-usuario").build()));
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        assertThatThrownBy(() -> service.create(
                registerId,
                MaintenanceRecordRequestDto.builder().assetId(assetId).description("x").build(),
                KEYCLOAK_USER,
                USER_EMAIL))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.userWithoutCompany().getErrorCode()));

        verify(maintenanceRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("create con keycloakUserId null lanza userWithoutCompany")
    void create_cuandoKeycloakUserIdNull_lanzaUserWithoutCompany() {
        when(maintenanceRegisterRepository.findById(registerId)).thenReturn(Optional.of(register));

        assertThatThrownBy(() -> service.create(
                registerId,
                MaintenanceRecordRequestDto.builder().assetId(assetId).description("x").build(),
                null,
                USER_EMAIL))
                .isInstanceOf(MaintenanceRegisterException.class)
                .satisfies(ex -> assertThat(((MaintenanceRegisterException) ex).getErrorCode())
                        .isEqualTo(MaintenanceRegisterException.userWithoutCompany().getErrorCode()));

        verify(maintenanceRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByRegister con assetId filtra por activo")
    void findByRegister_cuandoAssetIdPresente_usaRepositorioConAsset() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MaintenanceRecord> page = new PageImpl<>(List.of(new MaintenanceRecord()));
        when(maintenanceRecordRepository.findByMaintenanceRegisterIdAndAssetId(registerId, assetId, pageable))
                .thenReturn(page);
        when(maintenanceRecordMapper.toResponse(any(MaintenanceRecord.class)))
                .thenReturn(MaintenanceRecordResponseDto.builder().build());

        service.findByRegister(registerId, assetId, pageable);

        verify(maintenanceRecordRepository).findByMaintenanceRegisterIdAndAssetId(registerId, assetId, pageable);
        verify(maintenanceRecordRepository, never()).findByMaintenanceRegisterId(any(), any());
    }

    @Test
    @DisplayName("findByRegister sin assetId lista todo el register")
    void findByRegister_cuandoAssetIdNull_usaRepositorioPorRegister() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MaintenanceRecord> page = new PageImpl<>(List.of(new MaintenanceRecord()));
        when(maintenanceRecordRepository.findByMaintenanceRegisterId(registerId, pageable)).thenReturn(page);
        when(maintenanceRecordMapper.toResponse(any(MaintenanceRecord.class)))
                .thenReturn(MaintenanceRecordResponseDto.builder().build());

        service.findByRegister(registerId, null, pageable);

        verify(maintenanceRecordRepository).findByMaintenanceRegisterId(registerId, pageable);
        verify(maintenanceRecordRepository, never()).findByMaintenanceRegisterIdAndAssetId(any(), any(), any());
    }

    @Test
    @DisplayName("findByAsset delega al repositorio ordenado por fecha")
    void findByAsset_cuandoSeConsulta_usaFindByAssetIdOrderByCreatedAtDesc() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<MaintenanceRecord> page = new PageImpl<>(List.of());
        when(maintenanceRecordRepository.findByAssetIdOrderByCreatedAtDesc(assetId, pageable)).thenReturn(page);

        service.findByAsset(assetId, pageable);

        verify(maintenanceRecordRepository).findByAssetIdOrderByCreatedAtDesc(assetId, pageable);
        verifyNoInteractions(maintenanceRegisterRepository);
    }
}
