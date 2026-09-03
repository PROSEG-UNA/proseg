package com.proseg.msvc_document_processor.export.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.MaintenanceClient;
import com.proseg.msvc_document_processor.client.MaintenanceFeignExceptionTranslator;
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
public class TicketExportTarget implements DocumentExportTarget {

    private final MaintenanceClient maintenanceClient;
    private final MaintenanceFeignExceptionTranslator maintenanceErrorTranslator;

    private static final List<ExportColumnDefinition> COLUMNS = List.of(
            new ExportColumnDefinition("id", "ID", row -> getValue(row, "id")),
            new ExportColumnDefinition("title", "Titulo", row -> getValue(row, "title")),
            new ExportColumnDefinition("description", "Descripcion", row -> getValue(row, "description")),
            new ExportColumnDefinition("status", "Estado", row -> getValue(row, "status")),
            new ExportColumnDefinition("createdByName", "Creado por", row -> getValue(row, "createdByName")),
            new ExportColumnDefinition("createdBy", "ID creador", row -> getValue(row, "createdBy")),
            new ExportColumnDefinition("createdAt", "Creado", row -> getValue(row, "createdAt")),
            new ExportColumnDefinition("updatedAt", "Actualizado", row -> getValue(row, "updatedAt"))
    );

    @Override
    public String documentType() {
        return "tickets";
    }

    @Override
    public List<ExportColumnDefinition> columns() {
        return COLUMNS;
    }

    @Override
    public ExportPage fetchPage(MultiValueMap<String, String> queryParams) {
        try {
            ApiResponse<PageResponse<Map<String, Object>>> response = maintenanceClient.getTickets(queryParams);
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
