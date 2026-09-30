package com.proseg.msvc_document_processor.client;

import com.proseg.msvc_document_processor.exception.DocumentProcessorException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class InventoryFeignExceptionTranslator {

    public DocumentProcessorException translate(FeignException exception) {
        return DownstreamErrorTranslator.translate(
                exception,
                DocumentProcessorException::inventoryUnavailable,
                "INVENTORY_ERROR"
        );
    }
}
