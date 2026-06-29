package com.voum.modules.onboarding;

public interface StorageService {
    StoredFile uploadFile(String folder, String filename, byte[] content, String contentType);
    byte[] downloadFile(String fileUrl);
}
