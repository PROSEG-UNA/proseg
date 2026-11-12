package com.proseg.msvc_document_processor.export.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import com.proseg.msvc_document_processor.export.writer.ExcelExportWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransportExportTargetTest {

    @Test
    void writesVehicleColumnsInSpanish() throws Exception {
        TransportClient transportClient = mock(TransportClient.class);
        when(transportClient.getVehicles(any())).thenReturn(page(List.of(Map.of(
                "plate", "301-481",
                "type", "SEDAN",
                "capacity", 4,
                "available", false
        ))));
        VehicleExportTarget target = new VehicleExportTarget(transportClient);

        byte[] workbook = new ExcelExportWriter().write(target.columns(), target.fetchPage(new LinkedMultiValueMap<>()).rows());

        try (Workbook excel = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
            Row header = excel.getSheetAt(0).getRow(0);
            Row values = excel.getSheetAt(0).getRow(1);
            assertThat(cells(header)).containsExactly("Placa", "Tipo", "Capacidad", "Estado");
            assertThat(cells(values)).containsExactly("301-481", "Sedán", "4", "No disponible");
        }
    }

    @Test
    void writesBorrowedVehicleTypeInSpanish() throws Exception {
        TransportClient transportClient = mock(TransportClient.class);
        when(transportClient.getBorrowedVehicles(any())).thenReturn(page(List.of(Map.of(
                "plate", "400-100",
                "type", "MICROBUS",
                "capacity", 15
        ))));
        BorrowedVehicleExportTarget target = new BorrowedVehicleExportTarget(transportClient);

        byte[] workbook = new ExcelExportWriter().write(target.columns(), target.fetchPage(new LinkedMultiValueMap<>()).rows());

        try (Workbook excel = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
            assertThat(cells(excel.getSheetAt(0).getRow(0))).containsExactly("Placa", "Tipo", "Capacidad");
            assertThat(cells(excel.getSheetAt(0).getRow(1))).containsExactly("400-100", "Microbús", "15");
        }
    }

    @Test
    void writesBaseDriverColumnsWhenNoMonthIsSelected() throws Exception {
        TransportClient transportClient = mock(TransportClient.class);
        when(transportClient.getDrivers(any())).thenReturn(page(List.of(Map.of(
                "name", "Ana Mora",
                "organizationalUnit", "GENERAL_SERVICES",
                "status", "ACTIVE",
                "ordinaryStart", "07:30:00",
                "ordinaryEnd", "16:30:00"
        ))));
        DriverExportTarget target = new DriverExportTarget(transportClient);
        var page = target.fetchPage(new LinkedMultiValueMap<>());

        byte[] workbook = new ExcelExportWriter().write(target.columns(new LinkedMultiValueMap<>()), page.rows());

        try (Workbook excel = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
            assertThat(cells(excel.getSheetAt(0).getRow(0))).containsExactly("Nombre", "Unidad", "Estado", "Horario ordinario");
            assertThat(cells(excel.getSheetAt(0).getRow(1))).containsExactly("Ana Mora", "Servicios Generales", "Activo", "07:30 - 16:30");
        }
    }

    @Test
    void addsMonthColumnsOnlyWhenTheScreenSendsAMonth() throws Exception {
        TransportClient transportClient = mock(TransportClient.class);
        when(transportClient.getDrivers(any())).thenReturn(page(List.of(Map.of(
                "id", "driver-1",
                "name", "Ana Mora",
                "organizationalUnit", "OTHER_UNIT",
                "status", "INACTIVE",
                "ordinaryStart", "12:00:00",
                "ordinaryEnd", "20:00:00"
        ))));
        when(transportClient.getDriverMonths(2026, 5)).thenReturn(new ApiResponse<>("ok", List.of(Map.of(
                "driverId", "driver-1",
                "businessDays", 22,
                "ordinaryHoursAccrued", 12.5,
                "surplusHoursAccrued", 9.5,
                "surplusCap", 10,
                "overtimeHoursAccrued", 1,
                "weekendsWorked", 2
        )), 200));
        DriverExportTarget target = new DriverExportTarget(transportClient);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("year", "2026");
        params.add("month", "5");
        params.add("search", "Ana");
        var page = target.fetchPage(params);

        List<String> headers = target.columns(params).stream().map(ExportColumnDefinition::header).toList();
        Map<String, Object> row = page.rows().get(0);

        assertThat(headers).containsExactly(
                "Nombre", "Unidad", "Estado", "Horario ordinario",
                "Días Hábiles", "H. Ordinarias", "% Sobresueldo", "H. Sobresueldo", "H. Extra", "Fines de semana");
        assertThat(TransportExportLabels.unit(row.get("organizationalUnit"))).isEqualTo("Otra Unidad");
        assertThat(TransportExportLabels.driverStatus(row.get("status"))).isEqualTo("Inactivo");
        assertThat(TransportExportLabels.schedule(row.get("ordinaryStart"), row.get("ordinaryEnd"))).isEqualTo("12:00 - 20:00");
        assertThat(TransportExportLabels.whole(row.get("businessDays"))).isEqualTo("22");
        assertThat(TransportExportLabels.hours(row.get("ordinaryHoursAccrued"))).isEqualTo(TransportExportLabels.hours(12.5));
        assertThat(TransportExportLabels.percent(row.get("surplusExecutionRatio"))).isEqualTo(TransportExportLabels.percent(0.95));
        assertThat(row.get("weekendsWorked")).isEqualTo(2);
        verify(transportClient).getDrivers(eq(paramsWithoutMonth()));
    }

    private static MultiValueMap<String, String> paramsWithoutMonth() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("search", "Ana");
        return params;
    }

    private static ApiResponse<PageResponse<Map<String, Object>>> page(List<Map<String, Object>> rows) {
        return new ApiResponse<>("ok", PageResponse.<Map<String, Object>>builder()
                .content(rows)
                .last(true)
                .totalElements(rows.size())
                .build(), 200);
    }

    private static List<String> cells(Row row) {
        List<String> values = new java.util.ArrayList<>();
        row.forEach(cell -> values.add(cell.getStringCellValue()));
        return values;
    }
}
