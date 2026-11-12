package com.proseg.msvc_document_processor.export.target;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.DocumentExportTarget;
import com.proseg.msvc_document_processor.export.ExportColumnDefinition;
import com.proseg.msvc_document_processor.export.ExportPage;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DriverExportTarget implements DocumentExportTarget {

    private final TransportClient transportClient;

    private static final List<ExportColumnDefinition> BASE_COLUMNS = List.of(
            new ExportColumnDefinition("name", "Nombre", row -> row.get("name")),
            new ExportColumnDefinition("organizationalUnit", "Unidad", row -> TransportExportLabels.unit(row.get("organizationalUnit"))),
            new ExportColumnDefinition("status", "Estado", row -> TransportExportLabels.driverStatus(row.get("status"))),
            new ExportColumnDefinition("ordinarySchedule", "Horario ordinario", row -> TransportExportLabels.schedule(row.get("ordinaryStart"), row.get("ordinaryEnd")))
    );

    private static final List<ExportColumnDefinition> MONTH_COLUMNS = List.of(
            new ExportColumnDefinition("businessDays", "Días Hábiles", row -> TransportExportLabels.whole(row.get("businessDays"))),
            new ExportColumnDefinition("ordinaryHoursAccrued", "H. Ordinarias", row -> TransportExportLabels.hours(row.get("ordinaryHoursAccrued"))),
            new ExportColumnDefinition("surplusExecutionRatio", "% Sobresueldo", row -> TransportExportLabels.percent(row.get("surplusExecutionRatio"))),
            new ExportColumnDefinition("surplusHoursAccrued", "H. Sobresueldo", row -> TransportExportLabels.hours(row.get("surplusHoursAccrued"))),
            new ExportColumnDefinition("overtimeHoursAccrued", "H. Extra", row -> TransportExportLabels.hours(row.get("overtimeHoursAccrued"))),
            new ExportColumnDefinition("weekendsWorked", "Fines de semana", row -> TransportExportLabels.whole(row.get("weekendsWorked")))
    );

    @Override
    public String documentType() {
        return "drivers";
    }

    @Override
    public List<ExportColumnDefinition> columns() {
        return BASE_COLUMNS;
    }

    @Override
    public List<ExportColumnDefinition> columns(MultiValueMap<String, String> queryParams) {
        if (integerParam(queryParams, "year") == null || integerParam(queryParams, "month") == null) {
            return columns();
        }
        List<ExportColumnDefinition> columns = new ArrayList<>(BASE_COLUMNS);
        columns.addAll(MONTH_COLUMNS);
        return columns;
    }

    @Override
    public ExportPage fetchPage(MultiValueMap<String, String> queryParams) {
        Integer year = integerParam(queryParams, "year");
        Integer month = integerParam(queryParams, "month");
        boolean includeMonth = year != null && month != null;
        MultiValueMap<String, String> listParams = new LinkedMultiValueMap<>(queryParams);
        listParams.remove("year");
        listParams.remove("month");
        try {
            ExportPage page = TransportExportLabels.page(transportClient.getDrivers(listParams));
            if (!includeMonth) {
                return page;
            }
            Map<String, Map<String, Object>> months = driverMonths(year, month);
            List<Map<String, Object>> rows = page.rows().stream().map(row -> withMonth(row, months)).toList();
            return new ExportPage(rows, page.last(), page.totalElements());
        } catch (FeignException exception) {
            throw DownstreamErrorTranslator.translate(exception, DocumentProcessorException::transportExportFailed, "TRANSPORT_ERROR");
        }
    }

    private Map<String, Map<String, Object>> driverMonths(int year, int month) {
        ApiResponse<List<Map<String, Object>>> response = transportClient.getDriverMonths(year, month);
        List<Map<String, Object>> months = response == null || response.getData() == null ? List.of() : response.getData();
        Map<String, Map<String, Object>> byDriver = new LinkedHashMap<>();
        for (Map<String, Object> driverMonth : months) {
            if (driverMonth.get("driverId") != null) {
                byDriver.put(String.valueOf(driverMonth.get("driverId")), driverMonth);
            }
        }
        return byDriver;
    }

    private Map<String, Object> withMonth(Map<String, Object> row, Map<String, Map<String, Object>> months) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        Map<String, Object> driverMonth = row.get("id") == null ? null : months.get(String.valueOf(row.get("id")));
        double surplusHours = driverMonth == null ? 0 : TransportExportLabels.number(driverMonth.get("surplusHoursAccrued"));
        double surplusCap = driverMonth == null ? 0 : TransportExportLabels.number(driverMonth.get("surplusCap"));
        copy.put("businessDays", driverMonth == null ? null : driverMonth.get("businessDays"));
        copy.put("ordinaryHoursAccrued", driverMonth == null ? 0 : driverMonth.get("ordinaryHoursAccrued"));
        copy.put("surplusHoursAccrued", surplusHours);
        copy.put("overtimeHoursAccrued", driverMonth == null ? 0 : driverMonth.get("overtimeHoursAccrued"));
        copy.put("weekendsWorked", driverMonth == null ? 0 : driverMonth.get("weekendsWorked"));
        copy.put("surplusExecutionRatio", surplusCap > 0 ? surplusHours / surplusCap : null);
        return copy;
    }

    private static Integer integerParam(MultiValueMap<String, String> queryParams, String key) {
        String raw = queryParams.getFirst(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
