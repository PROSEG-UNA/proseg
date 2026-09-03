package com.proseg.msvc_document_processor.service;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.export.*;
import com.proseg.msvc_document_processor.export.writer.CsvExportWriter;
import com.proseg.msvc_document_processor.export.writer.ExcelExportWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentExportService {

    private static final String PAGE_PARAM = "page";
    private static final String SIZE_PARAM = "size";

    private final List<DocumentExportTarget> exportTargets;
    private final CsvExportWriter csvExportWriter;
    private final ExcelExportWriter excelExportWriter;
    private final ExportProperties exportProperties;

    public ExportResult export(String documentType, String formatValue, MultiValueMap<String, String> queryParams) {
        DocumentExportTarget target = resolveTarget(documentType);
        ExportFormat format = ExportFormat.fromValue(formatValue);
        List<Map<String, Object>> rows = collectRows(target, queryParams);

        byte[] content = switch (format) {
            case CSV -> csvExportWriter.write(target.columns(), rows);
            case XLSX -> excelExportWriter.write(target.columns(), rows);
        };

        return new ExportResult(format, content);
    }

    private List<Map<String, Object>> collectRows(DocumentExportTarget target, MultiValueMap<String, String> baseQueryParams) {
        List<Map<String, Object>> allRows = new ArrayList<>();
        int page = 0;
        int pageSize = resolvePageSize(baseQueryParams);
        int maxRows = Math.max(exportProperties.getMaxRows(), 1);

        while (true) {
            MultiValueMap<String, String> pageQuery = new LinkedMultiValueMap<>(baseQueryParams);
            pageQuery.set(PAGE_PARAM, String.valueOf(page));
            pageQuery.set(SIZE_PARAM, String.valueOf(pageSize));

            ExportPage exportPage = target.fetchPage(pageQuery);
            List<Map<String, Object>> pageRows = exportPage.rows() != null ? exportPage.rows() : List.of();

            allRows.addAll(pageRows);
            if (allRows.size() > maxRows) {
                throw DocumentProcessorException.exportMaxRowsExceeded(maxRows);
            }

            if (exportPage.last() || pageRows.isEmpty()) {
                break;
            }
            page++;
        }

        return allRows;
    }

    private int resolvePageSize(MultiValueMap<String, String> queryParams) {
        int fallback = Math.max(exportProperties.getPageSize(), 1);
        List<String> rawValues = queryParams.get(SIZE_PARAM);
        if (rawValues == null || rawValues.isEmpty()) {
            return fallback;
        }
        try {
            int requested = Integer.parseInt(rawValues.get(0));
            return requested > 0 ? requested : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private DocumentExportTarget resolveTarget(String documentType) {
        String normalized = documentType == null ? "" : documentType.trim().toLowerCase(Locale.ROOT);
        return exportTargets.stream()
                .filter(target -> target.documentType().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> DocumentProcessorException.unsupportedDocumentType(documentType));
    }
}
