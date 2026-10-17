package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.api.response.PageResponse;
import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftDto;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import com.proseg.msvc_document_processor.excel.TourSheetBlock;
import com.proseg.msvc_document_processor.excel.TourWorkbookGenerator;
import com.proseg.msvc_document_processor.export.TourDraftExport;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourDraftExportService {

    private static final String TRANSPORT_ERROR_CODE = "TRANSPORT_ERROR";
    private static final int PAGE_SIZE = 500;
    private static final String ROW_SORT = "rowNumber,asc";
    private static final String ROW_NUMBER_ATTRIBUTE = "rowNumber";
    private static final String DEPARTURE_DATE_ATTRIBUTE = "departureDate";
    private static final String FILE_SUFFIX = "_depurado.xlsx";
    private static final String FALLBACK_BASE_NAME = "borrador";
    private static final String SOURCE_EXTENSIONS = "(?i)(\\.(xlsx|xlsm|xls|csv))+$";
    private static final String INVALID_FILE_NAME_CHARACTERS = "[\\\\/:*?\"<>|]";
    private static final List<String> DAY_NAMES = List.of(
            "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"
    );
    private static final List<String> MONTH_NAMES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
    );

    private final TransportClient transportClient;
    private final TourWorkbookGenerator tourWorkbookGenerator;

    public TourDraftExport export(UUID draftId) {
        TourDraftDto draft = read(() -> transportClient.getCleaningDraft(draftId));
        SchemaDto schema = read(transportClient::getCleaningImportSchema);
        List<TourSheetBlock> blocks = dailyBlocks(fetchRows(draftId));
        return new TourDraftExport(fileName(draft), tourWorkbookGenerator.generate(schema, blocks));
    }

    private List<Map<String, Object>> fetchRows(UUID draftId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int page = 0; ; page++) {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("page", String.valueOf(page));
            queryParams.add("size", String.valueOf(PAGE_SIZE));
            queryParams.add("sort", ROW_SORT);
            PageResponse<Map<String, Object>> response = read(() -> transportClient.getCleaningDraftRows(draftId, queryParams));
            List<Map<String, Object>> content = response.getContent() == null ? List.of() : response.getContent();
            rows.addAll(content);
            if (response.isLast() || content.isEmpty()) {
                return rows;
            }
        }
    }

    private List<TourSheetBlock> dailyBlocks(List<Map<String, Object>> rows) {
        Map<LocalDate, List<Map<String, Object>>> rowsByDay = rows.stream()
                .sorted(Comparator.comparingInt(TourDraftExportService::rowNumber))
                .collect(Collectors.groupingBy(TourDraftExportService::departureDate, TreeMap::new, Collectors.toList()));
        return rowsByDay.entrySet().stream()
                .map(entry -> new TourSheetBlock(blockTitle(entry.getKey()), entry.getValue()))
                .toList();
    }

    private static int rowNumber(Map<String, Object> row) {
        return Integer.parseInt(String.valueOf(row.get(ROW_NUMBER_ATTRIBUTE)));
    }

    private static LocalDate departureDate(Map<String, Object> row) {
        return LocalDate.parse(String.valueOf(row.get(DEPARTURE_DATE_ATTRIBUTE)));
    }

    private String blockTitle(LocalDate date) {
        return DAY_NAMES.get(date.getDayOfWeek().getValue() - 1) + " "
                + date.getDayOfMonth() + " de "
                + MONTH_NAMES.get(date.getMonthValue() - 1) + " de "
                + date.getYear();
    }

    private String fileName(TourDraftDto draft) {
        Integer month = draft.getMonth();
        if (draft.getYear() != null && month != null && month >= 1 && month <= MONTH_NAMES.size()) {
            return MONTH_NAMES.get(month - 1).toLowerCase(Locale.ROOT) + " " + draft.getYear() + FILE_SUFFIX;
        }
        String sourceName = draft.getFileName() == null ? "" : draft.getFileName();
        String baseName = sourceName.replaceAll(SOURCE_EXTENSIONS, "")
                .replaceAll(INVALID_FILE_NAME_CHARACTERS, "_")
                .trim();
        return (baseName.isEmpty() ? FALLBACK_BASE_NAME : baseName) + FILE_SUFFIX;
    }

    private <T> T read(Supplier<ApiResponse<T>> request) {
        try {
            ApiResponse<T> response = request.get();
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.transportReadFailed();
            }
            return response.getData();
        } catch (FeignException e) {
            throw DownstreamErrorTranslator.translate(e, DocumentProcessorException::transportReadFailed, TRANSPORT_ERROR_CODE);
        }
    }
}
