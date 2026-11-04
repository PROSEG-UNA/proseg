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
import com.proseg.msvc_document_processor.excel.OvertimeRequestWorkbookGenerator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.DocumentFile;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OvertimeRequestExportServiceTest {

    private final FakeTransportClient transportClient = new FakeTransportClient();
    private final OvertimeRequestExportService service =
            new OvertimeRequestExportService(transportClient, new OvertimeRequestWorkbookGenerator());

    @Test
    void namesTheFileAfterTheMonthWithTwoDigitYear() {
        transportClient.report = report(2026, 9);

        DocumentFile file = service.export(2026, 9);

        assertThat(file.fileName()).isEqualTo("Solicitud de Horas Extra Setiembre 26.xlsx");
        assertThat(file.content()).isNotEmpty();
        assertThat(transportClient.requestedPeriod).containsExactly(2026, 9);
    }

    @Test
    void failsWhenTransportReturnsNoReport() {
        transportClient.report = null;

        assertThatThrownBy(() -> service.export(2026, 5)).isInstanceOf(DocumentProcessorException.class);
    }

    private OvertimeRequestReportDto report(int year, int month) {
        return OvertimeRequestReportDto.builder()
                .year(year)
                .month(month)
                .businessDays(21)
                .surplusCapPerDriver(28)
                .drivers(List.of())
                .build();
    }

    private static class FakeTransportClient implements TransportClient {

        private OvertimeRequestReportDto report;
        private List<Integer> requestedPeriod = List.of();

        @Override
        public ApiResponse<SchemaDto> getTourProcessingImportSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourDraftResultDto> createTourProcessingDraft(TourDraftRequestDto request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<TourDraftDto> getTourProcessingDraft(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResponse<PageResponse<Map<String, Object>>> getTourProcessingDraftRows(UUID id, MultiValueMap<String, String> queryParams) {
            throw new UnsupportedOperationException();
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
            requestedPeriod = List.of(year, month);
            return new ApiResponse<>("ok", report, 200);
        }
    }
}
