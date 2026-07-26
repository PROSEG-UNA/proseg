package com.sssi.msvc_document_processor.export.target;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_document_processor.client.MaintenanceClient;
import com.sssi.msvc_document_processor.client.MaintenanceFeignExceptionTranslator;
import com.sssi.msvc_document_processor.exception.DocumentProcessorException;
import com.sssi.msvc_document_processor.export.DocumentExportTarget;
import com.sssi.msvc_document_processor.export.ExportColumnDefinition;
import com.sssi.msvc_document_processor.export.ExportPage;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MaintenanceRequestExportTarget implements DocumentExportTarget {

    private final MaintenanceClient maintenanceClient;
    private final MaintenanceFeignExceptionTranslator maintenanceErrorTranslator;

    private static final List<ExportColumnDefinition> COLUMNS = List.of(
            new ExportColumnDefinition("id", "ID", row -> getValue(row, "id")),
            new ExportColumnDefinition("companyName", "Empresa", row -> getValue(row, "company", "name")),
            new ExportColumnDefinition("companyLegalId", "Cédula jurídica", row -> getValue(row, "company", "legalId")),
            new ExportColumnDefinition("description", "Descripción", row -> getValue(row, "description")),
            new ExportColumnDefinition("status", "Estado", row -> getValue(row, "status")),
            new ExportColumnDefinition("emails", "Correos electrónicos", row -> getValue(row, "emails")),
            new ExportColumnDefinition("startDate", "Fecha de inicio", row -> getValue(row, "startDate")),
            new ExportColumnDefinition("endDate", "Fecha de finalización", row -> getValue(row, "endDate")),
            new ExportColumnDefinition("startTime", "Hora de inicio", row -> getValue(row, "startTime")),
            new ExportColumnDefinition("endTime", "Hora de finalización", row -> getValue(row, "endTime")),
            new ExportColumnDefinition("campusId", "Campus", row -> getValue(row, "campusId")),
            new ExportColumnDefinition("buildingId", "Edificio", row -> getValue(row, "buildingId")),
            new ExportColumnDefinition("responsibleEmail", "Responsable", row -> getValue(row, "responsibleUserCompany", "userEmail")),
            new ExportColumnDefinition("assignedTechnicians", "Técnicos asignados", row -> getValue(row, "assignedTechnicians", "userEmail")),
            new ExportColumnDefinition("createdAt", "Fecha de creación", row -> getValue(row, "createdAt")),
            new ExportColumnDefinition("updatedAt", "Fecha de actualización", row -> getValue(row, "updatedAt"))
    );

    @Override
    public String documentType() {
        return "maintenance-requests";
    }

    @Override
    public List<ExportColumnDefinition> columns() {
        return COLUMNS;
    }

    @Override
    public ExportPage fetchPage(MultiValueMap<String, String> queryParams) {
        try {
            ApiResponse<PageResponse<Map<String, Object>>> response = maintenanceClient.getRequests(queryParams);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.maintenanceUnavailable();
            }

            PageResponse<Map<String, Object>> page = response.getData();
            List<Map<String, Object>> content = page.getContent() != null ? page.getContent() : List.of();
            return new ExportPage(content, page.isLast(), page.getTotalElements());
        } catch (FeignException e) {
            throw maintenanceErrorTranslator.translate(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getValue(Map<String, Object> source, String... path) {
        Object current = source;
        for (String key : path) {
            if (current instanceof List<?> list) {
                List<String> values = list.stream()
                        .map(item -> {
                            if (!(item instanceof Map<?, ?> map)) {
                                return String.valueOf(item);
                            }
                            Object value = ((Map<String, Object>) map).get(key);
                            return value != null ? String.valueOf(value) : null;
                        })
                        .filter(value -> value != null && !value.isBlank())
                        .toList();
                return values.isEmpty() ? null : String.join(" | ", values);
            }

            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof List<?> list) {
            return String.join(" | ", list.stream().map(String::valueOf).toList());
        }
        return current;
    }
}
