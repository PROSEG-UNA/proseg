package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.request.TourDraftRequestDto;
import com.proseg.msvc_document_processor.dto.response.FinalCommissionReportDto;
import com.proseg.msvc_document_processor.dto.response.TourControlReportDto;
import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftResultDto;
import com.proseg.msvc_document_processor.dto.response.TourProblemDto;
import com.proseg.msvc_document_processor.excel.TourSchemaFixture;
import com.proseg.msvc_document_processor.excel.TourWorkbookGenerator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.TourDraftExport;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
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
    private static final UUID PROBLEM_TOUR_ID = UUID.randomUUID();

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
    void asksTransportOnlyForApprovedTours() {
        transportClient.draft = draft(2026, 5, "1-mayo.xls");
        transportClient.pages = List.of(
                page(false, row(7, "B", "2026-05-02")),
                page(true, row(3, "A", "2026-05-02"))
        );

        service.export(DRAFT_ID);

        assertThat(transportClient.rowQueries)
                .extracting(query -> query.getFirst("approvalStatus"))
                .containsExactly("APPROVED", "APPROVED");
    }

    @Test
    void highlightsToursWithProblemsAndListsThemInTheLastColumn() throws IOException {
        transportClient.draft = draft(2026, 5, "1-mayo.xls");
        Map<String, Object> withProblem = row(3, "A", "2026-05-02");
        withProblem.put("id", PROBLEM_TOUR_ID.toString());
        Map<String, Object> withoutProblem = row(7, "B", "2026-05-02");
        withoutProblem.put("id", UUID.randomUUID().toString());
        transportClient.pages = List.of(page(true, withProblem, withoutProblem));
        transportClient.problems = Map.of(PROBLEM_TOUR_ID.toString(), List.of(
                new TourProblemDto("Sin chofer o sin vehículo", "La gira está aprobada pero ninguno de sus viajes tiene chofer y vehículo"),
                new TourProblemDto("Viajes a medias", "Algunos viajes de la gira no tienen chofer o vehículo")));

        TourDraftExport export = service.export(DRAFT_ID, true);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            Sheet sheet = workbook.getSheetAt(0);
            int problemsColumn = TourSchemaFixture.REAL_FILE_HEADERS.size();
            Row highlighted = sheet.getRow(2);
            Row plain = sheet.getRow(3);

            assertThat(sheet.getRow(1).getCell(problemsColumn).getStringCellValue()).isEqualTo("Problema presentado:");
            assertThat(highlighted.getCell(problemsColumn).getStringCellValue()).isEqualTo(
                    "Sin chofer o sin vehículo: La gira está aprobada pero ninguno de sus viajes tiene chofer y vehículo\n"
                            + "Viajes a medias: Algunos viajes de la gira no tienen chofer o vehículo");
            assertThat(highlighted.getCell(0).getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.YELLOW.getIndex());
            assertThat(highlighted.getCell(5).getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.YELLOW.getIndex());
            assertThat(highlighted.getCell(problemsColumn).getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.YELLOW.getIndex());
            assertThat(plain.getCell(problemsColumn)).isNull();
            assertThat(plain.getCell(3).getCellStyle().getFillPattern()).isEqualTo(FillPatternType.NO_FILL);
        }
        assertThat(transportClient.problemRequests).isEqualTo(1);
    }

    @Test
    void doesNotAskForProblemsUnlessHighlightingIsRequested() throws IOException {
        transportClient.draft = draft(2026, 5, "1-mayo.xls");
        transportClient.pages = List.of(page(true, row(3, "A", "2026-05-02")));

        TourDraftExport export = service.export(DRAFT_ID);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getLastCellNum()).isEqualTo((short) TourSchemaFixture.REAL_FILE_HEADERS.size());
        }
        assertThat(transportClient.problemRequests).isZero();
    }

    @Test
    void namesTheFileAfterTheDraftPeriod() {
        transportClient.draft = draft(2026, 9, "9-setiembre.xls");
        transportClient.pages = List.of(page(true, row(1, "A", "2026-09-01")));

        assertThat(service.export(DRAFT_ID).fileName()).isEqualTo("setiembre 2026_depurado.xlsx");
    }

    @Test
    void exportsEachAssignedVehicleWithItsOwnPassengerCount() throws IOException {
        transportClient.draft = draft(2026, 5, "1-mayo.xls");
        Map<String, Object> distributed = row(8, "03170", "2026-05-02");
        distributed.put("passengers", 70);
        distributed.put("assignments", List.of(
                Map.of("driver", "Daniel Zuniga", "vehicle", "301-569", "resourceType", "INTERNAL", "passengers", 40),
                Map.of("driver", "Marvin Quesada", "vehicle", "301-590", "resourceType", "INTERNAL", "passengers", 30)
        ));
        transportClient.pages = List.of(page(true, distributed));

        TourDraftExport export = service.export(DRAFT_ID);

        assertThat(firstColumnAndNumbers(export.content())).containsExactly(
                "Sabado 2 de Mayo de 2026", "x dia", "03170", "03170"
        );
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
        private Map<String, List<TourProblemDto>> problems = Map.of();
        private int problemRequests;

        @Override
        public ApiResponse<Map<String, List<TourProblemDto>>> getTourProcessingDraftRowProblems(UUID id) {
            problemRequests++;
            return new ApiResponse<>("ok", problems, 200);
        }

        @Override
        public ApiResponse<SchemaDto> getTourProcessingImportSchema() {
            return new ApiResponse<>("ok", TourSchemaFixture.schema(), 200);
        }

        @Override
        public ApiResponse<TourDraftResultDto> createTourProcessingDraft(TourDraftRequestDto request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourDraftDto> getTourProcessingDraft(UUID id) {
            return new ApiResponse<>("ok", draft, 200);
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getTourProcessingDraftRows(UUID id, MultiValueMap<String, String> queryParams) {
            rowQueries.add(queryParams);
            return new ApiResponse<>("ok", pages.get(rowQueries.size() - 1), 200);
        }

        @Override
        public ApiResponse<FinalCommissionReportDto> getFinalCommissionReport(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourControlReportDto> getTourControlReport(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourControlReportDto> getMonthTourControlReport(int year, int month) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<OvertimeRequestReportDto> getOvertimeRequestReport(int year, int month) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getVehicles(MultiValueMap<String, String> queryParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getBorrowedVehicles(MultiValueMap<String, String> queryParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getExternalVehicles(MultiValueMap<String, String> queryParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getDrivers(MultiValueMap<String, String> queryParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<List<Map<String, Object>>> getDriverMonths(int year, int month) {
            throw new UnsupportedOperationException();
        }
    }
}
