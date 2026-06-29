package com.voum.modules.onboarding;

public record StoredFile(
    String fileUrl,
    String fileName,
    String contentType,
    Long size
) {}
