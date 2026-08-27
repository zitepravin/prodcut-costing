package com.automotive.costing.common.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tenant-scoped file upload/download. Bytes never stored in MySQL.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class StoredFileController {

    private final StoredFileService storedFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestParam String entityType,
            @RequestParam(required = false) String entityId,
            @RequestPart("file") MultipartFile file) throws IOException {
        StoredFileEntity meta = storedFileService.store(entityType, entityId, file);
        return Map.of(
                "fileId", meta.getFileId(),
                "fileName", meta.getFileName(),
                "contentType", meta.getContentType() != null ? meta.getContentType() : "",
                "sizeBytes", meta.getSizeBytes(),
                "entityType", meta.getEntityType(),
                "entityId", meta.getEntityId() != null ? meta.getEntityId() : "",
                "status", meta.getStatus()
        );
    }

    @GetMapping("/{fileId}/metadata")
    public Map<String, Object> metadata(@PathVariable String fileId) {
        StoredFileEntity meta = storedFileService.getMetadata(fileId);
        return Map.of(
                "fileId", meta.getFileId(),
                "fileName", meta.getFileName(),
                "contentType", meta.getContentType() != null ? meta.getContentType() : "",
                "sizeBytes", meta.getSizeBytes(),
                "entityType", meta.getEntityType(),
                "entityId", meta.getEntityId() != null ? meta.getEntityId() : "",
                "status", meta.getStatus(),
                "storageProvider", meta.getStorageProvider()
        );
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) {
        StoredFileEntity meta = storedFileService.getMetadata(fileId);
        var content = storedFileService.openContent(fileId);
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null && !meta.getContentType().isBlank()) {
            try {
                mt = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .contentType(mt)
                .contentLength(meta.getSizeBytes())
                .body(new InputStreamResource(content.content()));
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam String entityType,
            @RequestParam String entityId) {
        return storedFileService.listForEntity(entityType, entityId).stream()
                .map(m -> Map.<String, Object>of(
                        "fileId", m.getFileId(),
                        "fileName", m.getFileName(),
                        "sizeBytes", m.getSizeBytes(),
                        "status", m.getStatus()
                ))
                .toList();
    }

    @DeleteMapping("/{fileId}")
    public Map<String, String> delete(@PathVariable String fileId) {
        storedFileService.delete(fileId);
        return Map.of("status", "DELETED", "fileId", fileId);
    }
}
