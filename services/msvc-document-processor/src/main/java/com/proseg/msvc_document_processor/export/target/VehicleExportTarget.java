package com.proseg.msvc_document_processor.export.target;

import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.DocumentExportTarget;
import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import com.proseg.msvc_document_processor.export.ExportPage;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VehicleExportTarget implements DocumentExportTarget {

    private final TransportClient transportClient;

    private static final List<ExportColumnDefinition> COLUMNS = List.of(
            new ExportColumnDefinition("plate", "Placa", row -> row.get("plate")),
            new ExportColumnDefinition("type", "Tipo", row -> TransportExportLabels.vehicleType(row.get("type"))),
            new ExportColumnDefinition("capacity", "Capacidad", row -> row.get("capacity")),
            new ExportColumnDefinition("available", "Estado", row -> TransportExportLabels.availability(row.get("available")))
    );

    @Override
    public String documentType() {
        return "vehicles";
    }

    @Override
    public List<ExportColumnDefinition> columns() {
        return COLUMNS;
    }

    @Override
    public ExportPage fetchPage(MultiValueMap<String, String> queryParams) {
        try {
            return TransportExportLabels.page(transportClient.getVehicles(queryParams));
        } catch (FeignException exception) {
            throw DownstreamErrorTranslator.translate(exception, DocumentProcessorException::transportExportFailed, "TRANSPORT_ERROR");
        }
    }
}
