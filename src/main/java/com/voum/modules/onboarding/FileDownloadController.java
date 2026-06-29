package com.voum.modules.onboarding;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/files")
@RequiredArgsConstructor
public class FileDownloadController {

    private final StorageService storageService;

    @GetMapping("/{folder}/{filename}")
    public ResponseEntity<byte[]> getFile(
            @PathVariable("folder") String folder,
            @PathVariable("filename") String filename) {
        
        String fileUrl = "/api/v1/storage/files/" + folder + "/" + filename;
        byte[] data = storageService.downloadFile(fileUrl);
        
        MediaType mediaType = getMediaType(filename);
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(data);
    }

    private MediaType getMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.IMAGE_JPEG;
    }
}
