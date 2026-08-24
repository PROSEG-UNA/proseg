package com.sssi.msvcinventory.service.impl;

import com.sssi.common.api.exception.BaseException;
import com.sssi.msvcinventory.dto.request.AssetImportRequestDto;
import com.sssi.msvcinventory.dto.request.AssetImportRowDto;
import com.sssi.msvcinventory.dto.request.ImportConfirmRequestDto;
import com.sssi.msvcinventory.dto.response.AssetColumnSchemaDto;
import com.sssi.msvcinventory.dto.response.AssetImportResponseDto;
import com.sssi.msvcinventory.dto.response.AssetImportRowIssueDto;
import com.sssi.msvcinventory.dto.response.AssetSchemaDto;
import com.sssi.msvcinventory.dto.response.ImportPreviewResponseDto;
import com.sssi.msvcinventory.dto.response.PendingCreationDto;
import com.sssi.msvcinventory.exception.AssetImportException;
import com.sssi.msvcinventory.importer.AssetImportField;
import com.sssi.msvcinventory.importer.ImportPeopleContext;
import com.sssi.msvcinventory.service.AssetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AssetImportServiceImpl implements AssetImportService {

    private static final int MAX_ROWS = 2000;

    private final AssetImportRowProcessor rowProcessor;

    @Override
    @Transactional(readOnly = true)
    public ImportPreviewResponseDto preview(AssetImportRequestDto request) {
        List<AssetImportRowDto> rows = request.getRows();
        if (rows.size() > MAX_ROWS) {
            throw AssetImportException.tooManyRows(MAX_ROWS);
        }

        List<AssetImportRowIssueDto> errors = validateAll(rows);
        List<PendingCreationDto> pendingCreations = errors.isEmpty()
                ? rowProcessor.planCreations(rows)
                : List.of();

        return ImportPreviewResponseDto.builder()
                .received(rows.size())
                .errors(errors)
                .pendingCreations(pendingCreations)
                .build();
    }

    @Override
    @Transactional
    public AssetImportResponseDto confirm(ImportConfirmRequestDto request) {
        List<AssetImportRowDto> rows = request.getRows();
        if (rows.size() > MAX_ROWS) {
            throw AssetImportException.tooManyRows(MAX_ROWS);
        }

        List<AssetImportRowIssueDto> errors = validateAll(rows);
        if (!errors.isEmpty()) {
            return AssetImportResponseDto.builder()
                    .received(rows.size())
                    .created(0)
                    .cancelled(false)
                    .errors(errors)
                    .build();
        }

        Set<String> approved = new HashSet<>(
                request.getApprovedKeys() == null ? List.of() : request.getApprovedKeys());
        List<PendingCreationDto> required = rowProcessor.planCreations(rows);
        boolean allApproved = required.stream().allMatch(creation -> approved.contains(creation.getKey()));
        if (!allApproved) {
            return AssetImportResponseDto.builder()
                    .received(rows.size())
                    .created(0)
                    .cancelled(true)
                    .errors(List.of())
                    .build();
        }

        rowProcessor.persistRows(rows);

        return AssetImportResponseDto.builder()
                .received(rows.size())
                .created(rows.size())
                .cancelled(false)
                .errors(List.of())
                .build();
    }

    @Override
    public AssetSchemaDto getAssetSchema() {
        List<AssetColumnSchemaDto> columns = Arrays.stream(AssetImportField.values())
                .map(field -> AssetColumnSchemaDto.builder()
                        .attribute(field.getAttribute())
                        .names(field.getNames())
                        .type(field.getType().name())
                        .build())
                .toList();

        return AssetSchemaDto.builder()
                .columns(columns)
                .build();
    }

    private List<AssetImportRowIssueDto> validateAll(List<AssetImportRowDto> rows) {
        Set<String> duplicateAssetNumbers = findDuplicatesInFile(rows, AssetImportRowDto::getAssetNumber);
        Set<String> duplicateSerials = findDuplicatesInFile(rows, AssetImportRowDto::getSerialNumber);
        Set<String> duplicateIpsInFile = findDuplicatesInFile(rows, AssetImportRowDto::getIpAddress);
        Set<String> duplicateMacsInFile = findDuplicatesInFile(rows, AssetImportRowDto::getMacAddress);

        ImportPeopleContext peopleContext = rowProcessor.newPeopleContext(false);

        List<AssetImportRowIssueDto> errors = new ArrayList<>();
        for (AssetImportRowDto row : rows) {
            try {
                rowProcessor.validateRow(row, duplicateAssetNumbers, duplicateSerials,
                        duplicateIpsInFile, duplicateMacsInFile, peopleContext);
            } catch (BaseException e) {
                errors.add(AssetImportRowIssueDto.builder()
                        .row(row.getRowNumber())
                        .reason(e.getMessage())
                        .build());
            }
        }
        return errors;
    }

    private Set<String> findDuplicatesInFile(List<AssetImportRowDto> rows,
                                             Function<AssetImportRowDto, String> extractor) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (AssetImportRowDto row : rows) {
            String value = trimToNull(extractor.apply(row));
            if (value != null && !seen.add(value)) {
                duplicates.add(value);
            }
        }
        return duplicates;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
