package com.proseg.msvc_transport.importer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum TransportImportField {

    DRIVER("driver", ColumnType.TEXT, false,
            List.of("Chofer")),
    NUMBER("number", ColumnType.TEXT, true,
            List.of("Numero")),
    VEHICLE("vehicle", ColumnType.TEXT, false,
            List.of("Vehículo")),
    PASSENGERS("passengers", ColumnType.INTEGER, true,
            List.of("Pasajeros")),
    EXECUTING_UNIT("executingUnit", ColumnType.TEXT, true,
            List.of("Unidad Ejecutora")),
    RESPONSIBLE("responsible", ColumnType.TEXT, true,
            List.of("Responsable")),
    DESTINATION("destination", ColumnType.TEXT, true,
            List.of("Destinos")),
    DURATION_DAYS("durationDays", ColumnType.INTEGER, true,
            List.of("Duración")),
    PRIORITY("priority", ColumnType.INTEGER, true,
            List.of("Prioridad")),
    MODALITY("modality", ColumnType.TEXT, false,
            List.of("Modalidad")),
    DEPARTURE_TIME("departureTime", ColumnType.TIME, true,
            List.of("Hora Salida", "Salida Hora", "Salida")),
    RETURN_TIME("returnTime", ColumnType.TIME, true,
            List.of("Hora Regreso", "Regreso Hora", "Regreso",
                    "Hora Entrada", "Entrada Hora", "Entrada")),
    DEPARTURE_DATE("departureDate", ColumnType.DATE, true,
            List.of("Fecha Salida", "Salida Fecha", "Salida")),
    RETURN_DATE("returnDate", ColumnType.DATE, true,
            List.of("Fecha Regreso", "Regreso Fecha", "Regreso",
                    "Fecha Entrada", "Entrada Fecha", "Entrada")),
    OBSERVATIONS("observations", ColumnType.TEXT, false,
            List.of("Observaciones"));

    private final String attribute;
    private final ColumnType type;
    private final boolean required;
    private final List<String> names;
}
