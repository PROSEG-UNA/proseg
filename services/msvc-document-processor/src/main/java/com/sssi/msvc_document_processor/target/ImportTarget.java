package com.sssi.msvc_document_processor.target;

import com.sssi.msvc_document_processor.dto.request.ConfirmRequestDto;
import com.sssi.msvc_document_processor.dto.response.ImportPreviewDto;
import com.sssi.msvc_document_processor.dto.request.RowsRequestDto;
import com.sssi.msvc_document_processor.dto.response.SchemaDto;
import com.sssi.msvc_document_processor.dto.response.ImportConfirmDto;

public interface ImportTarget {

    String documentType();

    SchemaDto fetchSchema();

    ImportPreviewDto preview(RowsRequestDto request);

    ImportConfirmDto confirm(ConfirmRequestDto request);
}
