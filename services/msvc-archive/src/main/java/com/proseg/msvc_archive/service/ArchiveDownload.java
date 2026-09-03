package com.proseg.msvc_archive.service;

import java.io.InputStream;

public record ArchiveDownload(
        String objectName,
        String contentType,
        long size,
        InputStream stream
) {
}