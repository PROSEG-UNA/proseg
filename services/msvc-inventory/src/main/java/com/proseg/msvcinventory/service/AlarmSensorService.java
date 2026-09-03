package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.AlarmSensorRequestDto;
import com.proseg.msvcinventory.dto.response.AlarmSensorResponseDto;

import java.util.UUID;

public interface AlarmSensorService {

    AlarmSensorResponseDto create(AlarmSensorRequestDto request);

    AlarmSensorResponseDto findById(UUID id);

    AlarmSensorResponseDto update(UUID id, AlarmSensorRequestDto request);
}
