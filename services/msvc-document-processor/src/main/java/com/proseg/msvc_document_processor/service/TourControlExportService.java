package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.response.TourControlReportDto;
import com.proseg.msvc_document_processor.excel.TourControlWorkbookGenerator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.DocumentFile;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TourControlExportService {

    private static final String TRANSPORT_ERROR_CODE = "TRANSPORT_ERROR";
    private static final String FILE_PREFIX = "Control de Giras ";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
    );

    private final TransportClient transportClient;
    private final TourControlWorkbookGenerator workbookGenerator;

    public DocumentFile export(UUID draftId) {
        return toFile(read(() -> transportClient.getTourControlReport(draftId)));
    }

    public DocumentFile export(int year, int month) {
        return toFile(read(() -> transportClient.getMonthTourControlReport(year, month)));
    }

    private DocumentFile toFile(TourControlReportDto report) {
        return new DocumentFile(fileName(report.getYear(), report.getMonth()), workbookGenerator.generate(report));
    }

    private String fileName(int year, int month) {
        return FILE_PREFIX + MONTH_NAMES.get(month - 1) + " " + String.format("%02d", year % 100) + FILE_EXTENSION;
    }

    private TourControlReportDto read(Supplier<ApiResponse<TourControlReportDto>> request) {
        try {
            ApiResponse<TourControlReportDto> response = request.get();
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.transportReadFailed();
            }
            return response.getData();
        } catch (FeignException e) {
            throw DownstreamErrorTranslator.translate(e, DocumentProcessorException::transportReadFailed, TRANSPORT_ERROR_CODE);
        }
    }
}
