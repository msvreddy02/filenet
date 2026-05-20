package com.mycompany.filenet.model;

import java.util.List;

import lombok.Data;

@Data
public class UploadResponse {

    private boolean success;
    private String message;
    private int totalFiles;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private List<FileResult> results;

    // ── Inner class: result per file ───────────────────────────────
    @Data
    public static class FileResult {

        private String filename;
        private String status;      // "SUCCESS", "FAILED", "SKIPPED"
        private String documentId;  // FileNet GUID (on success)
        private String message;     // error or skip reason
        private long sizeBytes;
        private long uploadTimeMs;
    }
}