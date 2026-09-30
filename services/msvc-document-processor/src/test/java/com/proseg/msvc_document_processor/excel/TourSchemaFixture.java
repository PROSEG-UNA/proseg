package com.proseg.msvc_document_processor.excel;

import com.proseg.msvc_document_processor.dto.response.ColumnSchemaDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;

import java.util.List;

public final class TourSchemaFixture {

    public static final List<String> REAL_FILE_HEADERS = List.of(
            "x dia", "x mes", "Chofer", "Numero", "Vehiculo", "Pasajeros", "Unidad Ejecutora", "Responsable",
            "Destinos", "Duracion", "Prioridad", "Modalidad", "Salida", "Regreso", "Salida", "Regreso", "Observaciones"
    );

    private TourSchemaFixture() {
    }

    public static SchemaDto schema() {
        return SchemaDto.builder()
                .columns(List.of(
                        column("driver", "TEXT", false, "Chofer"),
                        column("number", "TEXT", true, "Numero"),
                        column("vehicle", "TEXT", false, "Vehículo"),
                        column("passengers", "INTEGER", true, "Pasajeros"),
                        column("executingUnit", "TEXT", true, "Unidad Ejecutora"),
                        column("responsible", "TEXT", true, "Responsable"),
                        column("destination", "TEXT", true, "Destinos"),
                        column("durationDays", "INTEGER", true, "Duración"),
                        column("priority", "INTEGER", true, "Prioridad"),
                        column("modality", "TEXT", false, "Modalidad"),
                        column("departureTime", "TIME", true, "Hora Salida", "Salida Hora", "Salida"),
                        column("returnTime", "TIME", true, "Hora Regreso", "Regreso Hora", "Regreso",
                                "Hora Entrada", "Entrada Hora", "Entrada"),
                        column("departureDate", "DATE", true, "Fecha Salida", "Salida Fecha", "Salida"),
                        column("returnDate", "DATE", true, "Fecha Regreso", "Regreso Fecha", "Regreso",
                                "Fecha Entrada", "Entrada Fecha", "Entrada"),
                        column("observations", "TEXT", false, "Observaciones")
                ))
                .build();
    }

    private static ColumnSchemaDto column(String attribute, String type, boolean required, String... names) {
        return ColumnSchemaDto.builder()
                .attribute(attribute)
                .type(type)
                .required(required)
                .names(List.of(names))
                .build();
    }
}
