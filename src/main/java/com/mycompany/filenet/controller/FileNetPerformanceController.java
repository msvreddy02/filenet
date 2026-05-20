package com.mycompany.filenet.controller;

import com.mycompany.filenet.service.FileNetPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/performance")
@CrossOrigin(origins = "*")
public class FileNetPerformanceController {

    @Autowired
    private FileNetPerformanceService performanceService;

    // =====================================================
    // PERFORMANCE TEST API
    // =====================================================
    // POST:
    // http://localhost:8080/api/performance/upload
    //
    // form-data:
    // file   -> choose file
    // count  -> number of uploads
    // folder -> optional FileNet folder path
    // =====================================================

    @PostMapping("/upload")
    public ResponseEntity<String> performanceUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("count") int count,
            @RequestParam(value = "folder", required = false)
            String folder) {

        try {

            performanceService.uploadMultipleTimes(
                    file,
                    count,
                    folder
            );

            return ResponseEntity.ok(
                    "Performance upload completed successfully"
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }
}