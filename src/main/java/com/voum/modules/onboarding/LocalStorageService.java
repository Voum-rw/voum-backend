package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "pdf");
    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList("exe", "zip", "js", "sh");

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-dir:./voum-storage}") String localDir) {
        this.rootLocation = Paths.get(localDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage location", e);
        }
    }

    @Override
    public StoredFile uploadFile(String folder, String originalFilename, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new ApiException("File content cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        // 1. Validate File Size
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new ApiException("File size exceeds maximum limit of 10MB.", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate Extension
        String extension = getFileExtension(originalFilename);
        if (BLOCKED_EXTENSIONS.contains(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("File type not allowed. Allowed types: jpg, jpeg, png, pdf.", HttpStatus.BAD_REQUEST);
        }

        // 3. Save File
        try {
            Path folderPath = rootLocation.resolve(folder);
            Files.createDirectories(folderPath);

            String uniqueFilename = UUID.randomUUID() + "." + extension;
            Path destinationFile = folderPath.resolve(uniqueFilename);

            Files.write(destinationFile, content);

            String fileUrl = "/api/v1/storage/files/" + folder + "/" + uniqueFilename;

            return new StoredFile(fileUrl, uniqueFilename, contentType, (long) content.length);
        } catch (IOException e) {
            throw new ApiException("Failed to store file locally: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        try {
            // Extract folder and filename from URL path: /api/v1/storage/files/{folder}/{filename}
            String relativePath = fileUrl.replace("/api/v1/storage/files/", "");
            Path file = rootLocation.resolve(relativePath);
            
            if (Files.exists(file) && Files.isReadable(file)) {
                return Files.readAllBytes(file);
            } else {
                throw new ApiException("File not found or unreadable.", HttpStatus.NOT_FOUND);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to read file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
