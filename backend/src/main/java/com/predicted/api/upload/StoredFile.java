package com.predicted.api.upload;

public record StoredFile(
    String storagePath,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String sha256
) {
}
