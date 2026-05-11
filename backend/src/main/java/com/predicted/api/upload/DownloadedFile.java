package com.predicted.api.upload;

import org.springframework.core.io.Resource;

public record DownloadedFile(
    Resource resource,
    String filename,
    String contentType,
    long sizeBytes
) {
}
