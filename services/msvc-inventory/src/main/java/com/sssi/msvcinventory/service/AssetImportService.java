package com.sssi.msvcinventory.service;

import com.sssi.msvcinventory.dto.request.AssetImportRequestDto;
import com.sssi.msvcinventory.dto.request.ImportConfirmRequestDto;
import com.sssi.msvcinventory.dto.response.AssetImportResponseDto;
import com.sssi.msvcinventory.dto.response.AssetSchemaDto;
import com.sssi.msvcinventory.dto.response.ImportPreviewResponseDto;

public interface AssetImportService {

    ImportPreviewResponseDto preview(AssetImportRequestDto request);

    AssetImportResponseDto confirm(ImportConfirmRequestDto request);

    AssetSchemaDto getAssetSchema();
}
