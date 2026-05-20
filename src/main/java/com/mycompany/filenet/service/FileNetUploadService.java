package com.mycompany.filenet.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;


import com.mycompany.filenet.dto.FileDownloadResponse;
import com.mycompany.filenet.configuration.FileNetConnectionPool;
import com.mycompany.filenet.model.UploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.Subject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileNetUploadService {

    @Autowired
    private FileNetConnectionPool connectionPool;

    // =========================================================
    // DOWNLOAD DOCUMENT FROM FILENET
    // =========================================================
    public FileDownloadResponse downloadDocument(String documentId)
            throws Exception {

        // Get FileNet connection
        Connection connection = connectionPool.getConnection();

        // Create security subject
        Subject subject = UserContext.createSubject(
                connection,
                "wasadmin",          // FileNet username
                "password",          // FileNet password
                "FileNetP8WSI"
        );

        // Push security context
        UserContext.get().pushSubject(subject);

        try {

            ObjectStore objectStore = connectionPool.getObjectStore();

            // Fetch document from FileNet
            Document document = Factory.Document.fetchInstance(
                    objectStore,
                    new Id(documentId),
                    null
            );

            // Get content
            ContentTransfer ct =
                    (ContentTransfer) document.get_ContentElements().get(0);

            InputStream inputStream = ct.accessContentStream();

            byte[] content = inputStream.readAllBytes();

            // Build response
            FileDownloadResponse response =
                    new FileDownloadResponse();

            response.setContent(content);

            response.setFileName(
                    document.getProperties()
                            .getStringValue("DocumentTitle")
            );

            response.setContentType(ct.get_ContentType());

            return response;

        } finally {

            // Remove security context
            UserContext.get().popSubject();
        }
    }

    // =========================================================
    // MAX FILE SIZE
    // =========================================================
    private static final long MAX_SIZE =
            10L * 1024 * 1024 * 1024 * 1024;

    private static final List<String> BLOCKED_EXT =
            List.of(".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs");

    // =========================================================
    // UPLOAD MULTIPLE FILES
    // =========================================================
    public List<UploadResponse.FileResult> uploadMultipleFiles(
            MultipartFile[] files,
            String fileNetFolder) {

        List<UploadResponse.FileResult> results =
                new ArrayList<>();

        ObjectStore os = connectionPool.getObjectStore();

        for (MultipartFile file : files) {

            UploadResponse.FileResult result =
                    new UploadResponse.FileResult();

            result.setFilename(file.getOriginalFilename());
            result.setSizeBytes(file.getSize());

            try {

                long startTime =
                        System.currentTimeMillis();

                String docId =
                        uploadToFileNet(os, file, fileNetFolder);

                long endTime =
                        System.currentTimeMillis();

                long totalTime =
                        endTime - startTime;

                result.setStatus("SUCCESS");
                result.setDocumentId(docId);
                result.setMessage("Uploaded successfully");

                result.setUploadTimeMs(totalTime);

            } catch (Exception e) {

                result.setStatus("FAILED");
                result.setMessage(e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    // =========================================================
    // UPLOAD SINGLE FILE
    // =========================================================
    private String uploadToFileNet(
            ObjectStore os,
            MultipartFile file,
            String folderPath) throws Exception {

        // =====================================================
        // VALIDATE FILE
        // =====================================================

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException(
                    "File exceeds max size of 200MB"
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.isBlank()) {

            throw new RuntimeException(
                    "Invalid filename"
            );
        }

        // =====================================================
        // BLOCK DANGEROUS EXTENSIONS
        // =====================================================

        String lowerName =
                originalFilename.toLowerCase();

        for (String ext : BLOCKED_EXT) {

            if (lowerName.endsWith(ext)) {

                throw new RuntimeException(
                        "Blocked file type: " + ext
                );
            }
        }

        // =====================================================
        // GENERATE TIMESTAMP FILE NAME
        // Example:
        // report_20260512_123455.pdf
        // =====================================================

        String extension = "";

        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex != -1) {

            extension =
                    originalFilename.substring(dotIndex);

            originalFilename =
                    originalFilename.substring(0, dotIndex);
        }

        String timestamp =
                new java.text.SimpleDateFormat(
                        "yyyyMMdd_HHmmss"
                ).format(new java.util.Date());

        String finalFileName =
                originalFilename +
                        "_" +
                        timestamp +
                        extension;

        // =====================================================
        // CREATE DOCUMENT
        // =====================================================

        Document document =
                Factory.Document.createInstance(
                        os,
                        "Document"
                );

        document.getProperties().putValue(
                "DocumentTitle",
                finalFileName
        );

        // =====================================================
        // CONTENT TRANSFER
        // =====================================================

        ContentTransfer ct =
                Factory.ContentTransfer.createInstance();

        InputStream stream =
                file.getInputStream();

        ct.setCaptureSource(stream);

        ct.set_RetrievalName(finalFileName);

        ct.set_ContentType(
                file.getContentType() != null
                        ? file.getContentType()
                        : "application/octet-stream"
        );

        ContentElementList cel =
                Factory.ContentElement.createList();

        cel.add(ct);

        document.set_ContentElements(cel);

        // =====================================================
        // CHECKIN + SAVE
        // =====================================================

        document.checkin(
                AutoClassify.DO_NOT_AUTO_CLASSIFY,
                CheckinType.MAJOR_VERSION
        );

        document.save(RefreshMode.REFRESH);

        // =====================================================
        // FILE INTO FILENET FOLDER
        // =====================================================

        if (folderPath != null &&
                !folderPath.isBlank()) {

            Folder folder =
                    Factory.Folder.fetchInstance(
                            os,
                            folderPath,
                            null
                    );

            ReferentialContainmentRelationship rcr =
                    folder.file(
                            document,
                            AutoUniqueName.AUTO_UNIQUE,
                            finalFileName,
                            DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE
                    );

            rcr.save(RefreshMode.NO_REFRESH);
        }

        stream.close();

        return document.get_Id().toString();
    }
}