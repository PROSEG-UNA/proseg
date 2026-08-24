package com.sssi.msvc_document_processor.export.target;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.response.PageResponse;
import com.sssi.msvc_document_processor.client.InventoryClient;
import com.sssi.msvc_document_processor.client.InventoryFeignExceptionTranslator;
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
public class AssetExportTarget implements DocumentExportTarget {

    private final InventoryClient inventoryClient;
    private final InventoryFeignExceptionTranslator inventoryErrorTranslator;

    private static final List<ExportColumnDefinition> COLUMNS = List.of(
            new ExportColumnDefinition("assetNumber", "Número de activo", row -> getValue(row, "assetNumber")),
            new ExportColumnDefinition("kind", "Categoría", row -> getValue(row, "kind")),
            new ExportColumnDefinition("status", "Estado", row -> getValue(row, "status")),
            new ExportColumnDefinition("typeName", "Tipo", row -> getValue(row, "model", "type", "name")),
            new ExportColumnDefinition("brandName", "Marca", row -> getValue(row, "model", "brand", "name")),
            new ExportColumnDefinition("modelName", "Modelo", row -> getValue(row, "model", "name")),
            new ExportColumnDefinition("serialNumber", "Número de serie", row -> getValue(row, "serialNumber")),
            new ExportColumnDefinition("executingUnit", "Unidad ejecutora", row -> getValue(row, "executingUnit", "name")),
            new ExportColumnDefinition("responsibleEmployee", "Responsable", row -> getValue(row, "employee", "name")),
            new ExportColumnDefinition("responsibleEmployeeId", "Identificación del responsable", row -> getValue(row, "employee", "identification")),
            new ExportColumnDefinition("campus", "Campus", row -> getValue(row, "location", "floor", "building", "campus", "name")),
            new ExportColumnDefinition("building", "Edificio", row -> getValue(row, "location", "floor", "building", "name")),
            new ExportColumnDefinition("floor", "Piso", row -> getValue(row, "location", "floor", "name")),
            new ExportColumnDefinition("location", "Detalle de ubicación", row -> getValue(row, "location", "description")),
            new ExportColumnDefinition("acquisitionDate", "Fecha de adquisición", row -> getValue(row, "acquisitionDate")),
            new ExportColumnDefinition("warrantyEndDate", "Fin de garantía", row -> getValue(row, "warrantyEndDate")),
            new ExportColumnDefinition("firmwareSupportEndDate", "Fin del soporte de firmware", row -> getValue(row, "firmwareSupportEndDate")),
            new ExportColumnDefinition("decommissionDate", "Fecha de baja", row -> getValue(row, "decommissionDate")),
            new ExportColumnDefinition("ipAddress", "Dirección IP", row -> getValue(row, "networkInterface", "ipAddress")),
            new ExportColumnDefinition("macAddress", "Dirección MAC", row -> getValue(row, "networkInterface", "macAddress")),
            new ExportColumnDefinition("latitude", "Latitud", row -> getValue(row, "latitude")),
            new ExportColumnDefinition("longitude", "Longitud", row -> getValue(row, "longitude")),
            new ExportColumnDefinition("createdAt", "Fecha de creación", row -> getValue(row, "createdAt")),
            new ExportColumnDefinition("updatedAt", "Fecha de actualización", row -> getValue(row, "updatedAt"))
    );

    @Override
    public String documentType() {
        return "assets";
    }

    @Override
    public List<ExportColumnDefinition> columns() {
        return COLUMNS;
    }

    @Override
    public ExportPage fetchPage(MultiValueMap<String, String> queryParams) {
        try {
            ApiResponse<PageResponse<Map<String, Object>>> response = inventoryClient.getAssets(queryParams);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.inventoryUnavailable();
            }

            PageResponse<Map<String, Object>> page = response.getData();
            List<Map<String, Object>> content = page.getContent() != null ? page.getContent() : List.of();

            return new ExportPage(content, page.isLast(), page.getTotalElements());
        } catch (FeignException e) {
            throw inventoryErrorTranslator.translate(e);
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
