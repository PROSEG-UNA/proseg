package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.response.OvertimeRequestReportDto;
import com.proseg.msvc_document_processor.excel.OvertimeRequestWorkbookGenerator;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.DocumentFile;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeRequestExportService {

    private static final String TRANSPORT_ERROR_CODE = "TRANSPORT_ERROR";
    private static final String FILE_PREFIX = "Solicitud de Horas Extra ";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
    );

    private final TransportClient transportClient;
    private final OvertimeRequestWorkbookGenerator workbookGenerator;

    public DocumentFile export(int year, int month) {
        OvertimeRequestReportDto report = read(year, month);
        return new DocumentFile(fileName(year, month), workbookGenerator.generate(report));
    }

    private String fileName(int year, int month) {
        String shortYear = String.valueOf(year % 100);
        return FILE_PREFIX + MONTH_NAMES.get(month - 1) + " " + (shortYear.length() == 1 ? "0" + shortYear : shortYear) + FILE_EXTENSION;
    }

    private OvertimeRequestReportDto read(int year, int month) {
        try {
            ApiResponse<OvertimeRequestReportDto> response = transportClient.getOvertimeRequestReport(year, month);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.transportReadFailed();
            }
            return response.getData();
        } catch (FeignException e) {
            throw DownstreamErrorTranslator.translate(e, DocumentProcessorException::transportReadFailed, TRANSPORT_ERROR_CODE);
        }
    }
}
