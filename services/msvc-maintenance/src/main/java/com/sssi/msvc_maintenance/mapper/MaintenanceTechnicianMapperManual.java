package com.sssi.msvc_maintenance.mapper;

import com.sssi.msvc_maintenance.dto.request.MaintenanceTechnicianRequestDto;
import com.sssi.msvc_maintenance.dto.response.MaintenanceTechnicianResponseDto;
import com.sssi.msvc_maintenance.entity.MaintenanceTechnician;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MaintenanceTechnicianMapperManual implements MaintenanceTechnicianMapper {

    @Override
    public MaintenanceTechnician toEntity(MaintenanceTechnicianRequestDto request) {
        if (request == null) {
            return null;
        }

        return MaintenanceTechnician.builder()
                .fullName(request.getFullName())
                .position(request.getPosition())
                .email(request.getEmail())
                .phone(request.getPhone())
                .leader(request.isLeader())
                .keycloakUserId(request.getKeycloakUserId())
                .build();
    }

    @Override
    public MaintenanceTechnicianResponseDto toResponse(MaintenanceTechnician technician) {
        if (technician == null) {
            return null;
        }

        return MaintenanceTechnicianResponseDto.builder()
                .id(technician.getId())
                .fullName(technician.getFullName())
                .position(technician.getPosition())
                .email(technician.getEmail())
                .phone(technician.getPhone())
                .leader(technician.isLeader())
                .keycloakUserId(technician.getKeycloakUserId())
                .build();
    }

    @Override
    public void updateEntityFromRequest(MaintenanceTechnicianRequestDto request, MaintenanceTechnician technician) {
        if (request == null || technician == null) {
            return;
        }

        if (request.getFullName() != null) {
            technician.setFullName(request.getFullName());
        }
        if (request.getPosition() != null) {
            technician.setPosition(request.getPosition());
        }
        if (request.getEmail() != null) {
            technician.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            technician.setPhone(request.getPhone());
        }
        technician.setLeader(request.isLeader());
        if (request.getKeycloakUserId() != null) {
            technician.setKeycloakUserId(request.getKeycloakUserId());
        }
    }
}
