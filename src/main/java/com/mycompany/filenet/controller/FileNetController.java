package com.mycompany.filenet.controller;

import com.mycompany.filenet.dto.FileDownloadResponse;
import com.mycompany.filenet.service.FileNetUploadService;
import com.mycompany.filenet.model.UploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller — defines the HTTP endpoints.
 * These are the URLs you call from Postman.
 */
@RestController
@RequestMapping("/api/filenet")
@CrossOrigin(origins = "*")  // Allow requests from any origin
public class FileNetController {
    @GetMapping("/download/{documentId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String documentId) {

        try {

            FileDownloadResponse response =
                    uploadService.downloadDocument(documentId);

            return ResponseEntity.ok()
                    .header(
                            "Content-Disposition",
                            "attachment; filename=\"" + response.getFileName() + "\""
                    )
                    .header(
                            "Content-Type",
                            response.getContentType()
                    )
                    .body(response.getContent());

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(("Download failed: " + e.getMessage()).getBytes());
        }
    }

    @Autowired
    private FileNetUploadService uploadService;
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folder) {

        System.out.println("\n=== Upload Request Received ===");
        System.out.println("Number of files : " + files.length);
        System.out.println("Target folder   : " + (folder != null ? folder : "(none)"));

        // Validate at least one file was sent
        if (files == null || files.length == 0) {
            UploadResponse error = new UploadResponse();
            error.setSuccess(false);
            error.setMessage("No files provided. Add files in Postman under Body > form-data with key 'files'");
            return ResponseEntity.badRequest().body(error);
        }

        // Upload all files
        List<UploadResponse.FileResult> results =
                uploadService.uploadMultipleFiles(files, folder);

        // Build response summary
        UploadResponse response = new UploadResponse();
        response.setTotalFiles(results.size());
        response.setResults(results);

        long successCount = results.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failedCount  = results.stream()
                .filter(r -> "FAILED".equals(r.getStatus())).count();
        long skippedCount = results.stream()
                .filter(r -> "SKIPPED".equals(r.getStatus())).count();

        response.setSuccessCount((int) successCount);
        response.setFailedCount((int) failedCount);
        response.setSkippedCount((int) skippedCount);
        response.setSuccess(failedCount == 0);
        response.setMessage(String.format(
                "%d uploaded, %d failed, %d skipped",
                successCount, failedCount, skippedCount
        ));

        System.out.println("Response: " + response.getMessage());
        return ResponseEntity.ok(response);
    }

    // ────────────────────────────────────────────────────────────────
    // ENDPOINT 2: Upload a SINGLE file
    // POST http://localhost:8080/api/filenet/upload/single
    //
    // Postman setup:
    //   Method : POST
    //   URL    : http://localhost:8080/api/filenet/upload/single
    //   Body   : form-data
    //   Key    : file   (type = File)
    //   Key    : folder (type = Text, optional)
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/upload/single")
    public ResponseEntity<UploadResponse> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {

        return uploadMultipleFiles(new MultipartFile[]{file}, folder);
    }

    // ────────────────────────────────────────────────────────────────
    // ENDPOINT 3: Health check
    // GET http://localhost:8080/api/filenet/health
    // Use this to verify the server is running before uploading
    // ────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(
                "{\"status\":\"running\",\"message\":\"FileNet API is up\"}"
        );
    }
}
