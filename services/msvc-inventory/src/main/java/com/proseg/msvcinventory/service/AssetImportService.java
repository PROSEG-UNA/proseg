package com.proseg.msvcinventory.service;

import com.proseg.msvcinventory.dto.request.AssetImportRequestDto;
import com.proseg.msvcinventory.dto.request.ImportConfirmRequestDto;
import com.proseg.msvcinventory.dto.response.AssetImportResponseDto;
import com.proseg.msvcinventory.dto.response.AssetSchemaDto;
import com.proseg.msvcinventory.dto.response.ImportPreviewResponseDto;

public interface AssetImportService {

    ImportPreviewResponseDto preview(AssetImportRequestDto request);

    AssetImportResponseDto confirm(ImportConfirmRequestDto request);

    AssetSchemaDto getAssetSchema();
}
