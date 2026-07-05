package com.sssi.msvc_document_processor.export;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "document-processor.export")
public class ExportProperties {

    private int pageSize = 500;
    private int maxRows = 50000;
}
