package com.proseg.msvcinventory.service.impl;

import com.proseg.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.proseg.msvcinventory.dto.response.AlarmSensorResponseDto;
import com.proseg.msvcinventory.dto.request.AssetRequestDto;
import com.proseg.msvcinventory.entity.AlarmSensor;
import com.proseg.msvcinventory.entity.Model;
import com.proseg.msvcinventory.entity.Location;
import com.proseg.msvcinventory.entity.ExecutingUnit;
import com.proseg.msvcinventory.entity.Employee;
import com.proseg.msvcinventory.entity.enums.AssetStatus;
import com.proseg.msvcinventory.exception.AlarmSensorException;
import com.proseg.msvcinventory.exception.AssetException;
import com.proseg.msvcinventory.exception.ModelException;
import com.proseg.msvcinventory.exception.LocationException;
import com.proseg.msvcinventory.exception.ExecutingUnitException;
import com.proseg.msvcinventory.exception.EmployeeException;
import com.proseg.msvcinventory.mapper.AlarmSensorMapper;
import com.proseg.msvcinventory.repository.AlarmSensorRepository;
import com.proseg.msvcinventory.repository.ModelRepository;
import com.proseg.msvcinventory.repository.LocationRepository;
import com.proseg.msvcinventory.repository.ExecutingUnitRepository;
import com.proseg.msvcinventory.repository.EmployeeRepository;
import com.proseg.msvcinventory.service.AlarmSensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlarmSensorServiceImpl implements AlarmSensorService {

    private final AlarmSensorRepository alarmSensorRepository;
    private final ModelRepository modelRepository;
    private final LocationRepository locationRepository;
    private final ExecutingUnitRepository executingUnitRepository;
    private final EmployeeRepository employeeRepository;
    private final AlarmSensorMapper alarmSensorMapper;

    @Override
    @Transactional
    public AlarmSensorResponseDto create(AlarmSensorRequestDto request) {
        request.setSerialNumber(trimToNull(request.getSerialNumber()));

        validateDecommissionDate(request);

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> ModelException.notFound(request.getModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        AlarmSensor alarmSensor = alarmSensorMapper.toEntity(request);
        alarmSensor.setModel(model);
        alarmSensor.setLocation(location);
        alarmSensor.setExecutingUnit(resolveExecutingUnit(request.getExecutingUnitId()));
        alarmSensor.setEmployee(resolveEmployee(request.getEmployeeId()));

        return alarmSensorMapper.toResponse(alarmSensorRepository.save(alarmSensor));
    }

    @Override
    @Transactional(readOnly = true)
    public AlarmSensorResponseDto findById(UUID id) {
        return alarmSensorRepository.findById(id)
                .map(alarmSensorMapper::toResponse)
                .orElseThrow(() -> AlarmSensorException.notFound(id.toString()));
    }

    @Override
    @Transactional
    public AlarmSensorResponseDto update(UUID id, AlarmSensorRequestDto request) {
        AlarmSensor alarmSensor = alarmSensorRepository.findById(id)
                .orElseThrow(() -> AlarmSensorException.notFound(id.toString()));

        request.setSerialNumber(trimToNull(request.getSerialNumber()));

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> ModelException.notFound(request.getModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        validateDecommissionDate(request);

        alarmSensorMapper.updateEntityFromRequest(request, alarmSensor);
        alarmSensor.setModel(model);
        alarmSensor.setLocation(location);
        alarmSensor.setExecutingUnit(resolveExecutingUnit(request.getExecutingUnitId()));
        alarmSensor.setEmployee(resolveEmployee(request.getEmployeeId()));

        return alarmSensorMapper.toResponse(alarmSensorRepository.save(alarmSensor));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExecutingUnit resolveExecutingUnit(UUID executingUnitId) {
        if (executingUnitId == null) return null;
        return executingUnitRepository.findById(executingUnitId)
                .orElseThrow(() -> ExecutingUnitException.notFound(executingUnitId.toString()));
    }

    private Employee resolveEmployee(UUID employeeId) {
        if (employeeId == null) return null;
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> EmployeeException.notFound(employeeId.toString()));
    }

    private void validateDecommissionDate(AssetRequestDto request) {
        if (request.getStatus() == AssetStatus.APROBADO && request.getDecommissionDate() != null) {
            throw AssetException.decommissionDateNotAllowed();
        }
    }
}
