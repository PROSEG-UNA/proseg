package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto;
import com.proseg.msvc_maintenance.dto.response.InventoryBuildingEmailResponseDto;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.MaintenanceEmail;
import com.proseg.msvc_maintenance.entity.MaintenanceRequest;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.entity.enums.MaintenanceStatus;
import com.proseg.msvc_maintenance.mapper.MaintenanceRequestMapper;
import com.proseg.msvc_maintenance.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceImplTest {

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
    private com.proseg.msvc_maintenance.client.InventoryClient inventoryClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MaintenanceRequestServiceImpl service;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private MaintenanceRequest existingRequest;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        existingRequest = new MaintenanceRequest();
        existingRequest.setId(requestId);
        existingRequest.setCompany(new Company());
        existingRequest.setDescription("desc");
        existingRequest.setStartDate(LocalDate.now());
        existingRequest.setEndDate(LocalDate.now());
        existingRequest.setStartTime(LocalTime.now());
        existingRequest.setEndTime(LocalTime.now());
        existingRequest.setEmails(List.of(MaintenanceEmail.builder().email("a@b.com").build()));
    }

    @Test
    void cancel_should_publish_rejected_notification_once() {
        existingRequest.setStatus(MaintenanceStatus.PENDING);
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(requestId, "motivo");

        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        Object ev = eventCaptor.getValue();
        assertThat(ev).isInstanceOf(com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent.class);
        com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent ne = (com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent) ev;
        assertThat(ne.actionType()).isEqualTo("REJECTED");
        assertThat(ne.cancellationReason()).isEqualTo("motivo");
    }

    @Test
    void update_transition_to_cancelled_should_publish_rejected_once() {
        UUID companyId = UUID.randomUUID();
        UUID campusId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();

        existingRequest.setStatus(MaintenanceStatus.PENDING);
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubUpdateDependencies(companyId, campusId, technicianId);

        // Simulate mapper updating entity from request to set status CANCELLED and cancellation reason
        doAnswer(inv -> {
            MaintenanceRequestRequestDto req = inv.getArgument(0);
            MaintenanceRequest entity = inv.getArgument(1);
            entity.setStatus(MaintenanceStatus.CANCELLED);
            entity.setCancellationReason("motivo-update");
            return null;
        }).when(maintenanceRequestMapper).updateEntityFromRequest(any(), any());

        MaintenanceRequestRequestDto reqDto = MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .startTime(LocalTime.now())
                .endTime(LocalTime.now())
                .campusId(campusId.toString())
                .assignedTechnicianIds(List.of(technicianId))
                .emails(List.of("a@b.com"))
                .status(MaintenanceStatus.CANCELLED)
                .build();

        service.update(requestId, reqDto);

        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        Object ev = eventCaptor.getValue();
        assertThat(ev).isInstanceOf(com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent.class);
        com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent ne = (com.proseg.msvc_maintenance.event.MaintenanceRequestNotificationDomainEvent) ev;
        assertThat(ne.actionType()).isEqualTo("REJECTED");
        assertThat(ne.cancellationReason()).isEqualTo("motivo-update");
    }

    @Test
    void update_when_already_cancelled_should_not_publish_rejection_again() {
        UUID companyId = UUID.randomUUID();
        UUID campusId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();

        existingRequest.setStatus(MaintenanceStatus.CANCELLED);
        existingRequest.setCancellationReason("already");
        when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubUpdateDependencies(companyId, campusId, technicianId);

        doNothing().when(maintenanceRequestMapper).updateEntityFromRequest(any(), any());

        MaintenanceRequestRequestDto reqDto = MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .startTime(LocalTime.now())
                .endTime(LocalTime.now())
                .campusId(campusId.toString())
                .assignedTechnicianIds(List.of(technicianId))
                .emails(List.of("a@b.com"))
                .status(MaintenanceStatus.CANCELLED)
                .build();

        service.update(requestId, reqDto);

        verify(eventPublisher, times(0)).publishEvent(any());
    }

    private void stubUpdateDependencies(UUID companyId, UUID campusId, UUID technicianId) {
        MaintenanceRequestServiceImplTestSupport.stubUpdateDependencies(
                companyRepository,
                userCompanyRepository,
                maintenanceEmailRepository,
                inventoryClient,
                maintenanceRequestMapper,
                companyId,
                campusId,
                technicianId);
    }
}
