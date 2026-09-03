package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.BuildingEmailRequestDto;
import com.proseg.msvcinventory.dto.response.BuildingEmailResponseDto;
import com.proseg.msvcinventory.entity.Building;
import com.proseg.msvcinventory.entity.BuildingEmail;
import com.proseg.msvcinventory.exception.BuildingEmailException;
import com.proseg.msvcinventory.exception.BuildingException;
import com.proseg.msvcinventory.exception.CampusException;
import com.proseg.msvcinventory.mapper.BuildingEmailMapper;
import com.proseg.msvcinventory.repository.BuildingEmailRepository;
import com.proseg.msvcinventory.repository.BuildingRepository;
import com.proseg.msvcinventory.repository.CampusRepository;
import com.proseg.msvcinventory.service.BuildingEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingEmailServiceImpl implements BuildingEmailService {

    private final BuildingEmailRepository buildingEmailRepository;
    private final BuildingRepository buildingRepository;
    private final CampusRepository campusRepository;
    private final BuildingEmailMapper buildingEmailMapper;

    @Override
    @Transactional
    public BuildingEmailResponseDto create(UUID buildingId, BuildingEmailRequestDto request) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> BuildingException.notFound(buildingId.toString()));

        if (buildingEmailRepository.existsByBuildingIdAndEmail(buildingId, request.getEmail())) {
            throw BuildingEmailException.duplicateEmail(request.getEmail());
        }

        BuildingEmail buildingEmail = BuildingEmail.builder()
                .email(request.getEmail())
                .building(building)
                .build();

        return buildingEmailMapper.toResponse(buildingEmailRepository.save(buildingEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildingEmailResponseDto> findByBuildingId(UUID buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw BuildingException.notFound(buildingId.toString());
        }
        return buildingEmailRepository.findByBuildingId(buildingId)
                .stream()
                .map(buildingEmailMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildingEmailResponseDto> findByCampusId(UUID campusId) {
        if (!campusRepository.existsById(campusId)) {
            throw CampusException.notFound(campusId.toString());
        }
        return buildingEmailRepository.findByBuildingCampusId(campusId)
                .stream()
                .map(buildingEmailMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID emailId) {
        BuildingEmail buildingEmail = buildingEmailRepository.findById(emailId)
                .orElseThrow(() -> BuildingEmailException.notFound(emailId.toString()));
        buildingEmailRepository.delete(buildingEmail);
    }
}