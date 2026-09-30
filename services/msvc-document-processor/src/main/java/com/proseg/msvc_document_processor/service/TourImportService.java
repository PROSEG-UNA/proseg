package com.proseg.msvc_document_processor.service;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.msvc_document_processor.client.DownstreamErrorTranslator;
import com.proseg.msvc_document_processor.client.TransportClient;
import com.proseg.msvc_document_processor.dto.request.TourDraftRequestDto;
import com.proseg.msvc_document_processor.dto.response.RowIssueDto;
import com.proseg.msvc_document_processor.dto.response.SchemaDto;
import com.proseg.msvc_document_processor.dto.response.TourDraftResultDto;
import com.proseg.msvc_document_processor.dto.response.TourImportSummaryDto;
import com.proseg.msvc_document_processor.excel.ParseResult;
import com.proseg.msvc_document_processor.excel.TourTemplateGenerator;
import com.proseg.msvc_document_processor.excel.block.DailyBlockSheetParser;
import com.proseg.msvc_document_processor.excel.reader.TabularFile;
import com.proseg.msvc_document_processor.excel.reader.TabularFileReader;
import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourImportService {

    private static final String TRANSPORT_ERROR_CODE = "TRANSPORT_ERROR";

    private final TabularFileReader tabularFileReader;
    private final DailyBlockSheetParser dailyBlockSheetParser;
    private final TourTemplateGenerator tourTemplateGenerator;
    private final TransportClient transportClient;

    public TourImportSummaryDto importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw DocumentProcessorException.emptyFile();
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        TabularFile tabularFile = tabularFileReader.read(fileName, readBytes(file));
        ParseResult parsed = dailyBlockSheetParser.parse(tabularFile.rows(), fetchSchema());
        String sourceFormat = tabularFile.format().name();

        if (!parsed.getErrors().isEmpty()) {
            return summary(parsed, sourceFormat, null, 0, parsed.getErrors());
        }
        if (parsed.getRows().isEmpty()) {
            List<RowIssueDto> noRows = List.of(RowIssueDto.builder()
                    .reason("El archivo no contiene filas de giras")
                    .build());
            return summary(parsed, sourceFormat, null, 0, noRows);
        }

        TourDraftResultDto draft = createDraft(TourDraftRequestDto.builder()
                .fileName(fileName)
                .fileType(sourceFormat)
                .totalRowsRead(parsed.getTotalRows())
                .blocksDetected(parsed.getBlocksDetected())
                .rows(parsed.getRows())
                .build());
        List<RowIssueDto> errors = new ArrayList<>(draft.getErrors() == null ? List.of() : draft.getErrors());
        errors.sort(Comparator.comparing(RowIssueDto::getRow, Comparator.nullsFirst(Comparator.naturalOrder())));
        return summary(parsed, sourceFormat, errors.isEmpty() ? draft.getDraftId() : null, draft.getDuplicateRows(), errors);
    }

    public byte[] generateTemplate() {
        return tourTemplateGenerator.generate(fetchSchema());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw DocumentProcessorException.unreadableWorkbook();
        }
    }

    private SchemaDto fetchSchema() {
        try {
            ApiResponse<SchemaDto> response = transportClient.getCleaningImportSchema();
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.transportUnavailable();
            }
            return response.getData();
        } catch (FeignException e) {
            throw DownstreamErrorTranslator.translate(e, DocumentProcessorException::transportUnavailable, TRANSPORT_ERROR_CODE);
        }
    }

    private TourDraftResultDto createDraft(TourDraftRequestDto request) {
        try {
            ApiResponse<TourDraftResultDto> response = transportClient.createCleaningDraft(request);
            if (response == null || response.getData() == null) {
                throw DocumentProcessorException.transportUnavailable();
            }
            return response.getData();
        } catch (FeignException e) {
            throw DownstreamErrorTranslator.translate(e, DocumentProcessorException::transportUnavailable, TRANSPORT_ERROR_CODE);
        }
    }

    private TourImportSummaryDto summary(
            ParseResult parsed,
            String sourceFormat,
            UUID draftId,
            int duplicateRows,
            List<RowIssueDto> errors
    ) {
        return TourImportSummaryDto.builder()
                .totalRows(parsed.getTotalRows())
                .blocksDetected(parsed.getBlocksDetected())
                .sourceFormat(sourceFormat)
                .draftId(draftId)
                .duplicateRows(duplicateRows)
                .errors(errors)
                .build();
    }
}
