package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.response.TourControlReportDto;
import com.proseg.msvc_document_processor.excel.TourControlWorkbookGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourControlExportServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void exportsTheTourControlOfAMonth() {
        TourControlReportDto report = mock(TourControlReportDto.class);
        when(report.getYear()).thenReturn(2026);
        when(report.getMonth()).thenReturn(5);
        ApiResponse<TourControlReportDto> response = mock(ApiResponse.class);
        when(response.getData()).thenReturn(report);
        TransportClient transportClient = mock(TransportClient.class);
        when(transportClient.getMonthTourControlReport(2026, 5)).thenReturn(response);
        TourControlWorkbookGenerator workbookGenerator = mock(TourControlWorkbookGenerator.class);
        byte[] content = {1, 2, 3};
        when(workbookGenerator.generate(report)).thenReturn(content);

        var file = new TourControlExportService(transportClient, workbookGenerator).export(2026, 5);

        assertThat(file.fileName()).contains("Mayo 26");
        assertThat(file.content()).isEqualTo(content);
    }
}
