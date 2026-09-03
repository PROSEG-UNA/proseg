package com.proseg.msvcinventory.exception;

import com.proseg.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AlarmSensorException extends BaseException {

    public AlarmSensorException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AlarmSensorException notFound(String id) {
        return new AlarmSensorException(
                HttpStatus.NOT_FOUND,
                "ALARM_SENSOR_NOT_FOUND",
                "Sensor de alarma no encontrado con id: " + id
        );
    }
}
