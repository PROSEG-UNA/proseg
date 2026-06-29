package com.sssi.msvc_document_processor.service;

import com.sssi.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.sssi.msvc_document_processor.dto.response.ImportPreviewDto;
import com.sssi.msvc_document_processor.dto.response.PreviewSummaryDto;
import com.sssi.msvc_document_processor.dto.request.RowsRequestDto;
import com.sssi.msvc_document_processor.dto.response.SchemaDto;
import com.sssi.msvc_document_processor.dto.response.SummaryDto;
import com.sssi.msvc_document_processor.dto.response.ImportConfirmDto;
import com.sssi.msvc_document_processor.dto.response.RowIssueDto;
import com.sssi.msvc_document_processor.excel.ExcelTemplateGenerator;
import com.sssi.msvc_document_processor.excel.GenericExcelParser;
import com.sssi.msvc_document_processor.excel.ParseResult;
import com.sssi.msvc_document_processor.exception.DocumentProcessorException;
import com.sssi.msvc_document_processor.target.ImportTarget;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentImportService {

    private final GenericExcelParser parser;
    private final ExcelTemplateGenerator templateGenerator;
    private final Map<String, ImportTarget> targets;

    public DocumentImportService(GenericExcelParser parser,
                                 ExcelTemplateGenerator templateGenerator,
                                 List<ImportTarget> importTargets) {
        this.parser = parser;
        this.templateGenerator = templateGenerator;
        this.targets = importTargets.stream()
                .collect(Collectors.toMap(ImportTarget::documentType, target -> target));
    }

    public byte[] generateTemplate(String documentType) {
        ImportTarget target = resolveTarget(documentType);
        SchemaDto schema = target.fetchSchema();
        return templateGenerator.generate(schema);
    }

    public PreviewSummaryDto preview(String documentType, MultipartFile file) {
        ImportTarget target = resolveTarget(documentType);
        validateFile(file);

        SchemaDto schema = target.fetchSchema();

        ParseResult parsed;
        try {
            parsed = parser.parse(file.getInputStream(), schema);
        } catch (IOException e) {
            throw DocumentProcessorException.unreadableWorkbook();
        }

        if (!parsed.getErrors().isEmpty()) {
            return PreviewSummaryDto.builder()
                    .totalRows(parsed.getTotalRows())
                    .errors(sortByRow(parsed.getErrors()))
                    .pendingCreations(List.of())
                    .rows(List.of())
                    .build();
        }

        ImportPreviewDto preview = target.preview(
                RowsRequestDto.builder().rows(parsed.getRows()).build());

        List<RowIssueDto> errors = new ArrayList<>();
        if (preview.getErrors() != null) {
            errors.addAll(preview.getErrors());
        }

        return PreviewSummaryDto.builder()
                .totalRows(parsed.getTotalRows())
                .errors(sortByRow(errors))
                .pendingCreations(preview.getPendingCreations() != null ? preview.getPendingCreations() : List.of())
                .rows(errors.isEmpty() ? parsed.getRows() : List.of())
                .build();
    }

    public SummaryDto confirm(String documentType, ConfirmRequestDto request) {
        ImportTarget target = resolveTarget(documentType);

        ImportConfirmDto response = target.confirm(request);

        List<RowIssueDto> errors = new ArrayList<>();
        if (response.getErrors() != null) {
            errors.addAll(response.getErrors());
        }

        int totalRows = request.getRows() != null ? request.getRows().size() : 0;

        return SummaryDto.builder()
                .totalRows(totalRows)
                .created(response.getCreated())
                .cancelled(response.isCancelled())
                .errors(sortByRow(errors))
                .build();
    }

    private ImportTarget resolveTarget(String documentType) {
        ImportTarget target = targets.get(documentType);
        if (target == null) {
            throw DocumentProcessorException.unsupportedDocumentType(documentType);
        }
        return target;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw DocumentProcessorException.emptyFile();
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw DocumentProcessorException.invalidFileType();
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw DocumentProcessorException.invalidFileType();
        }
    }

    private List<RowIssueDto> sortByRow(List<RowIssueDto> messages) {
        messages.sort(Comparator.comparing(RowIssueDto::getRow,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return messages;
    }
}
