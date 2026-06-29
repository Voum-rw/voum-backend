package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void uploadFile_withValidImage_shouldSucceed() {
        byte[] content = "fake-image-bytes".getBytes();
        StoredFile file = storageService.uploadFile("profile", "photo.png", content, "image/png");

        assertNotNull(file);
        assertTrue(file.fileUrl().startsWith("/api/v1/storage/files/profile/"));
        assertTrue(file.fileName().endsWith(".png"));
        assertEquals("image/png", file.contentType());
        assertEquals((long) content.length, file.size());
    }

    @Test
    void uploadFile_withBlockedExtension_shouldThrowException() {
        byte[] content = "fake-payload".getBytes();
        ApiException exception = assertThrows(ApiException.class, () -> 
                storageService.uploadFile("docs", "script.sh", content, "application/x-sh"));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("File type not allowed"));
    }

    @Test
    void uploadFile_exceedingMaxSize_shouldThrowException() {
        // Create content larger than 10MB
        byte[] largeContent = new byte[10 * 1024 * 1024 + 1];
        
        ApiException exception = assertThrows(ApiException.class, () -> 
                storageService.uploadFile("docs", "document.pdf", largeContent, "application/pdf"));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("exceeds maximum limit"));
    }
}
