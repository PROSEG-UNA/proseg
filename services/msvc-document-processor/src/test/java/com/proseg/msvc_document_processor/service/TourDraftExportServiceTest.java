package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.request.TourDraftRequestDto;
import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftResultDto;
import com.proseg.msvc_document_processor.excel.TourSchemaFixture;
import com.proseg.msvc_document_processor.excel.TourWorkbookGenerator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.TourDraftExport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourDraftExportServiceTest {

    private static final UUID DRAFT_ID = UUID.randomUUID();

    private final FakeTransportClient transportClient = new FakeTransportClient();
    private final TourDraftExportService service = new TourDraftExportService(transportClient, new TourWorkbookGenerator());

    @Test
    void groupsAllPagesByDepartureDayInRowOrder() throws IOException {
        transportClient.draft = draft(2026, 5, "1-mayo.xls");
        transportClient.pages = List.of(
                page(false, row(40, "D", "2026-05-04"), row(7, "B", "2026-05-02")),
                page(true, row(12, "C", "2026-05-04"), row(3, "A", "2026-05-02"), row(90, "E", "2026-05-31"))
        );

        TourDraftExport export = service.export(DRAFT_ID);

        assertThat(firstColumnAndNumbers(export.content())).containsExactly(
                "Sabado 2 de Mayo de 2026", "x dia", "A", "B",
                "Lunes 4 de Mayo de 2026", "x dia", "C", "D",
                "Domingo 31 de Mayo de 2026", "x dia", "E"
        );
        assertThat(transportClient.rowQueries)
                .extracting(query -> query.getFirst("page"))
                .containsExactly("0", "1");
        assertThat(transportClient.rowQueries.get(0).getFirst("sort")).isEqualTo("rowNumber,asc");
    }

    @Test
    void namesTheFileAfterTheDraftPeriod() {
        transportClient.draft = draft(2026, 9, "9-setiembre.xls");
        transportClient.pages = List.of(page(true, row(1, "A", "2026-09-01")));

        assertThat(service.export(DRAFT_ID).fileName()).isEqualTo("setiembre 2026_depurado.xlsx");
    }

    @Test
    void namesTheFileAfterTheSourceFileWhenThePeriodIsMissing() {
        transportClient.draft = draft(null, null, "1-mayo.xls.xls");
        transportClient.pages = List.of(page(true));

        assertThat(service.export(DRAFT_ID).fileName()).isEqualTo("1-mayo_depurado.xlsx");
    }

    @Test
    void failsWhenTransportReturnsNoDraft() {
        transportClient.draft = null;

        assertThatThrownBy(() -> service.export(DRAFT_ID)).isInstanceOf(DocumentProcessorException.class);
    }

    private List<String> firstColumnAndNumbers(byte[] content) throws IOException {
        List<String> values = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                values.add(row.getCell(0) != null ? row.getCell(0).getStringCellValue() : row.getCell(3).getStringCellValue());
            }
        }
        return values;
    }

    private TourDraftDto draft(Integer year, Integer month, String fileName) {
        return TourDraftDto.builder()
                .id(DRAFT_ID)
                .fileName(fileName)
                .year(year)
                .month(month)
                .build();
    }

    @SafeVarargs
    private PageResponse<Map<String, Object>> page(boolean last, Map<String, Object>... rows) {
        PageResponse<Map<String, Object>> page = new PageResponse<>();
        page.setContent(List.of(rows));
        page.setLast(last);
        return page;
    }

    private Map<String, Object> row(int rowNumber, String number, String departureDate) {
        Map<String, Object> row = new HashMap<>();
        row.put("rowNumber", rowNumber);
        row.put("number", number);
        row.put("departureDate", departureDate);
        return row;
    }

    private static class FakeTransportClient implements TransportClient {

        private TourDraftDto draft;
        private List<PageResponse<Map<String, Object>>> pages = List.of();
        private final List<MultiValueMap<String, String>> rowQueries = new ArrayList<>();

        @Override
        public ApiResponse<SchemaDto> getCleaningImportSchema() {
            return new ApiResponse<>("ok", TourSchemaFixture.schema(), 200);
        }

        @Override
        public ApiResponse<TourDraftResultDto> createCleaningDraft(TourDraftRequestDto request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourDraftDto> getCleaningDraft(UUID id) {
            return new ApiResponse<>("ok", draft, 200);
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getCleaningDraftRows(UUID id, MultiValueMap<String, String> queryParams) {
            rowQueries.add(queryParams);
            return new ApiResponse<>("ok", pages.get(rowQueries.size() - 1), 200);
        }

        @Override
        public ApiResponse<OvertimeRequestReportDto> getOvertimeRequestReport(int year, int month) {
            throw new UnsupportedOperationException();
        }
    }
}
