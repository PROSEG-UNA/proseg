package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.sssi.msvcinventory.dto.response.AlarmSensorResponseDto;
import com.sssi.msvcinventory.entity.AlarmSensor;
import com.sssi.msvcinventory.entity.AssetModel;
import com.sssi.msvcinventory.entity.Location;
import com.sssi.msvcinventory.exception.AlarmSensorException;
import com.sssi.msvcinventory.exception.AssetModelException;
import com.sssi.msvcinventory.exception.LocationException;
import com.sssi.msvcinventory.mapper.AlarmSensorMapper;
import com.sssi.msvcinventory.repository.AlarmSensorRepository;
import com.sssi.msvcinventory.repository.AssetModelRepository;
import com.sssi.msvcinventory.repository.LocationRepository;
import com.sssi.msvcinventory.service.AlarmSensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlarmSensorServiceImpl implements AlarmSensorService {

    private final AlarmSensorRepository alarmSensorRepository;
    private final AssetModelRepository assetModelRepository;
    private final LocationRepository locationRepository;
    private final AlarmSensorMapper alarmSensorMapper;

    @Override
    @Transactional
    public AlarmSensorResponseDto create(AlarmSensorRequestDto request) {
        AssetModel assetModel = assetModelRepository.findById(request.getAssetModelId())
                .orElseThrow(() -> AssetModelException.notFound(request.getAssetModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        AlarmSensor alarmSensor = alarmSensorMapper.toEntity(request);
        alarmSensor.setAssetModel(assetModel);
        alarmSensor.setLocation(location);

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

        AssetModel assetModel = assetModelRepository.findById(request.getAssetModelId())
                .orElseThrow(() -> AssetModelException.notFound(request.getAssetModelId().toString()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> LocationException.notFound(request.getLocationId().toString()));

        alarmSensorMapper.updateEntityFromRequest(request, alarmSensor);
        alarmSensor.setAssetModel(assetModel);
        alarmSensor.setLocation(location);

        return alarmSensorMapper.toResponse(alarmSensorRepository.save(alarmSensor));
    }
}
