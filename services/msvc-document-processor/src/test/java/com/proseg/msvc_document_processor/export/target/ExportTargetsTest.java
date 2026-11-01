package com.proseg.msvc_document_processor.export.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.InventoryClient;
import com.proseg.msvc_document_processor.client.InventoryFeignExceptionTranslator;
import com.proseg.msvc_document_processor.client.MaintenanceClient;
import com.proseg.msvc_document_processor.client.MaintenanceFeignExceptionTranslator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExportTargetsTest {

    @Test
    void asset_target_maps_pages_and_nested_fields() {
        InventoryClient client = mock(InventoryClient.class);
        AssetExportTarget target = new AssetExportTarget(client, mock(InventoryFeignExceptionTranslator.class));
        Map<String, Object> row = Map.of(
                "assetNumber", "A-1",
                "kind", List.of("laptop", "office"),
                "model", Map.of("type", Map.of("name", "Equipo"), "brand", Map.of("name", "Marca"), "name", "Modelo"),
                "location", Map.of("floor", Map.of("building", Map.of("campus", Map.of("name", "Campus"),
                        "name", "Edificio"), "name", "Piso"), "description", "Sala"),
                "tags", List.of("a", "b"));
        PageResponse<Map<String, Object>> data = page(List.of(row), true, 1);
        when(client.getAssets(any())).thenReturn(new ApiResponse<>("ok", data, 200));

        var result = target.fetchPage(new LinkedMultiValueMap<>());
        Map<String, Object> values = extractors(target.columns(), row);

        assertThat(target.documentType()).isEqualTo("assets");
        assertThat(result.rows()).containsExactly(row);
        assertThat(result.last()).isTrue();
        assertThat(values).containsEntry("Número de activo", "A-1")
                .containsEntry("Categoría", "laptop | office")
                .containsEntry("Tipo", "Equipo")
                .containsEntry("Marca", "Marca")
                .containsEntry("Campus", "Campus")
                .containsEntry("Edificio", "Edificio")
                .containsEntry("Piso", "Piso");
        assertThat(target.columns().stream().filter(c -> c.key().equals("tags")).findFirst()).isEmpty();
    }

    @Test
    void ticket_target_extracts_nested_values_and_joins_lists() {
        MaintenanceClient client = mock(MaintenanceClient.class);
        TicketExportTarget target = new TicketExportTarget(client, mock(MaintenanceFeignExceptionTranslator.class));
        Map<String, Object> row = Map.of("id", "T-1", "title", List.of("uno", "dos"));
        when(client.getTickets(any())).thenReturn(new ApiResponse<>("ok", page(List.of(row), false, 1), 200));

        assertThat(target.documentType()).isEqualTo("tickets");
        assertThat(target.fetchPage(new LinkedMultiValueMap<>()).last()).isFalse();
        assertThat(extractors(target.columns(), row))
                .containsEntry("ID", "T-1")
                .containsEntry("Titulo", "uno | dos")
                .containsEntry("Estado", null);
        assertThat(extractors(target.columns(), Map.of("title", List.of("uno", "dos"))))
                .containsEntry("Titulo", "uno | dos");
    }

    @Test
    void maintenance_request_target_extracts_nested_lists_and_skips_empty_list_values() {
        MaintenanceClient client = mock(MaintenanceClient.class);
        MaintenanceRequestExportTarget target = new MaintenanceRequestExportTarget(client, mock(MaintenanceFeignExceptionTranslator.class));
        Map<String, Object> row = Map.of(
                "company", Map.of("name", "Empresa", "legalId", "3-101"),
                "emails", List.of("uno@example.com", "dos@example.com"),
                "assignedTechnicians", List.of("asignación sin mapa",
                        Map.of("userEmail", "tech@example.com"),
                        Map.of("userEmail", " "),
                        Map.of("otro", "sin correo")),
                "responsibleUserCompany", Map.of("userEmail", "responsable@example.com"));
        when(client.getRequests(any())).thenReturn(new ApiResponse<>("ok", page(List.of(row), true, 1), 200));

        assertThat(target.documentType()).isEqualTo("maintenance-requests");
        assertThat(extractors(target.columns(), row))
                .containsEntry("Empresa", "Empresa")
                .containsEntry("Cédula jurídica", "3-101")
                .containsEntry("Correos electrónicos", "uno@example.com | dos@example.com")
                .containsEntry("Responsable", "responsable@example.com")
                .containsEntry("Técnicos asignados", "asignación sin mapa | tech@example.com");
        assertThat(extractors(target.columns(), Map.of("assignedTechnicians", List.of())))
                .containsEntry("Técnicos asignados", null);
    }

    @Test
    void returns_empty_content_for_null_page_content_and_errors_for_missing_data() {
        InventoryClient inventory = mock(InventoryClient.class);
        AssetExportTarget assetTarget = new AssetExportTarget(inventory, mock(InventoryFeignExceptionTranslator.class));
        when(inventory.getAssets(any())).thenReturn(new ApiResponse<>("ok", page(null, true, 0), 200));
        assertThat(assetTarget.fetchPage(new LinkedMultiValueMap<>()).rows()).isEmpty();
        when(inventory.getAssets(any())).thenReturn(null);
        assertThatThrownBy(() -> assetTarget.fetchPage(new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);

        MaintenanceClient maintenance = mock(MaintenanceClient.class);
        TicketExportTarget ticketTarget = new TicketExportTarget(maintenance, mock(MaintenanceFeignExceptionTranslator.class));
        when(maintenance.getTickets(any())).thenReturn(new ApiResponse<>("ok", null, 200));
        assertThatThrownBy(() -> ticketTarget.fetchPage(new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
        MaintenanceRequestExportTarget requestTarget =
                new MaintenanceRequestExportTarget(maintenance, mock(MaintenanceFeignExceptionTranslator.class));
        when(maintenance.getRequests(any())).thenReturn(null);
        assertThatThrownBy(() -> requestTarget.fetchPage(new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
        when(maintenance.getRequests(any())).thenReturn(new ApiResponse<>("ok", null, 200));
        assertThatThrownBy(() -> requestTarget.fetchPage(new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);

        when(maintenance.getTickets(any())).thenReturn(new ApiResponse<>("ok", page(null, true, 0), 200));
        assertThat(ticketTarget.fetchPage(new LinkedMultiValueMap<>()).rows()).isEmpty();
        when(maintenance.getTickets(any())).thenReturn(null);
        assertThatThrownBy(() -> ticketTarget.fetchPage(new LinkedMultiValueMap<>()))
                .isInstanceOf(DocumentProcessorException.class);
    }

    @Test
    void translates_feign_failures_from_both_clients() {
        FeignException failure = mock(FeignException.class);
        var inventory = mock(InventoryClient.class);
        var inventoryTranslator = mock(InventoryFeignExceptionTranslator.class);
        var asset = new AssetExportTarget(inventory, inventoryTranslator);
        var inventoryError = DocumentProcessorException.inventoryUnavailable();
        when(inventory.getAssets(any())).thenThrow(failure);
        when(inventoryTranslator.translate(failure)).thenReturn(inventoryError);
        assertThatThrownBy(() -> asset.fetchPage(new LinkedMultiValueMap<>())).isSameAs(inventoryError);

        var maintenance = mock(MaintenanceClient.class);
        var maintenanceTranslator = mock(MaintenanceFeignExceptionTranslator.class);
        var ticket = new TicketExportTarget(maintenance, maintenanceTranslator);
        var request = new MaintenanceRequestExportTarget(maintenance, maintenanceTranslator);
        var maintenanceError = DocumentProcessorException.maintenanceUnavailable();
        when(maintenance.getTickets(any())).thenThrow(failure);
        when(maintenance.getRequests(any())).thenThrow(failure);
        when(maintenanceTranslator.translate(failure)).thenReturn(maintenanceError);
        assertThatThrownBy(() -> ticket.fetchPage(new LinkedMultiValueMap<>())).isSameAs(maintenanceError);
        assertThatThrownBy(() -> request.fetchPage(new LinkedMultiValueMap<>())).isSameAs(maintenanceError);
    }

    private static Map<String, Object> extractors(List<ExportColumnDefinition> columns, Map<String, Object> row) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (ExportColumnDefinition column : columns) {
            values.put(column.header(), column.valueExtractor().apply(row));
        }
        return values;
    }

    private static <T> PageResponse<T> page(List<T> content, boolean last, long total) {
        return PageResponse.<T>builder().content(content).last(last).totalElements(total).build();
    }
}
