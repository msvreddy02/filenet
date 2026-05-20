package com.mycompany.filenet.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;

import com.mycompany.filenet.configuration.FileNetConnectionPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class FileNetPerformanceService {

    @Autowired
    private FileNetConnectionPool connectionPool;

    public void uploadMultipleTimes(
            MultipartFile file,
            int count,
            String folderPath) throws Exception {

        ObjectStore os =
                connectionPool.getObjectStore();

        long overallStart =
                System.currentTimeMillis();

        long totalUploadedBytes = 0;

        System.out.println(
                "\n======================================="
        );

        System.out.println(
                "PERFORMANCE TEST STARTED"
        );

        System.out.println(
                "File Name : "
                        + file.getOriginalFilename()
        );

        System.out.println(
                "Upload Count : "
                        + count
        );

        System.out.println(
                "Single File Size : "
                        + file.getSize()
        );

        System.out.println(
                "======================================="
        );

        for (int i = 1; i <= count; i++) {

            InputStream stream = null;

            try {

                long start =
                        System.currentTimeMillis();

                String inTime =
                        new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss.SSS"
                        ).format(new Date());

                // =====================================
                // FILE NAME
                // =====================================

                String originalName =
                        file.getOriginalFilename();

                String extension = "";

                int dotIndex =
                        originalName.lastIndexOf(".");

                if (dotIndex != -1) {

                    extension =
                            originalName.substring(dotIndex);

                    originalName =
                            originalName.substring(
                                    0,
                                    dotIndex
                            );
                }

                String timestamp =
                        new SimpleDateFormat(
                                "yyyyMMdd_HHmmss_SSS"
                        ).format(new Date());

                String finalFileName =
                        originalName +
                                "_" +
                                i +
                                "_" +
                                timestamp +
                                extension;

                // SAFE FILE NAME
                finalFileName =
                        finalFileName.replaceAll(
                                "[^a-zA-Z0-9._-]",
                                "_"
                        );

                // =====================================
                // CREATE DOCUMENT
                // =====================================

                Document document =
                        Factory.Document.createInstance(
                                os,
                                "Document"
                        );

                document.getProperties().putValue(
                        "DocumentTitle",
                        finalFileName
                );

                // =====================================
                // CONTENT TRANSFER
                // =====================================

                ContentTransfer ct =
                        Factory.ContentTransfer
                                .createInstance();

                stream =
                        file.getInputStream();

                ct.setCaptureSource(stream);

                ct.set_RetrievalName(
                        finalFileName
                );

                ct.set_ContentType(
                        file.getContentType() != null
                                ? file.getContentType()
                                : "application/octet-stream"
                );

                ContentElementList cel =
                        Factory.ContentElement
                                .createList();

                cel.add(ct);

                document.set_ContentElements(cel);

                // =====================================
                // CHECKIN
                // =====================================

                document.checkin(
                        AutoClassify.DO_NOT_AUTO_CLASSIFY,
                        CheckinType.MAJOR_VERSION
                );

                // =====================================
                // SAVE
                // =====================================

                System.out.println(
                        "\nUploading iteration : " + i
                );

                document.save(
                        RefreshMode.REFRESH
                );

                // =====================================
                // FILE INTO FOLDER
                // =====================================

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
                                    DefineSecurityParentage
                                            .DO_NOT_DEFINE_SECURITY_PARENTAGE
                            );

                    rcr.save(
                            RefreshMode.NO_REFRESH
                    );
                }

                long end =
                        System.currentTimeMillis();

                long totalTime =
                        end - start;

                totalUploadedBytes +=
                        file.getSize();

                String outTime =
                        new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss.SSS"
                        ).format(new Date());

                // =====================================
                // SUCCESS LOGGING
                // =====================================

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "Iteration : " + i
                );

                System.out.println(
                        "File Name : "
                                + finalFileName
                );

                System.out.println(
                        "Document Id : "
                                + document.get_Id()
                );

                System.out.println(
                        "In Time : "
                                + inTime
                );

                System.out.println(
                        "Out Time : "
                                + outTime
                );

                System.out.println(
                        "Upload Time(ms) : "
                                + totalTime
                );

                System.out.println(
                        "======================================"
                );

            } catch (Exception e) {

                System.out.println(
                        "\n========= UPLOAD FAILED ========="
                );

                System.out.println(
                        "Iteration : " + i
                );

                System.out.println(
                        "Error : "
                                + e.getMessage()
                );

                e.printStackTrace();

                throw e;

            } finally {

                // =====================================
                // FORCE STREAM CLEANUP
                // =====================================

                try {

                    if (stream != null) {
                        stream.close();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // =====================================
                // HELP GC
                // =====================================

                System.gc();

                // =====================================
                // SMALL DELAY
                // Prevent MTOM/CE overload
                // =====================================

                Thread.sleep(3000);
            }
        }

        // ============================================
        // FINAL SUMMARY
        // ============================================

        long overallEnd =
                System.currentTimeMillis();

        long totalExecution =
                overallEnd - overallStart;

        double totalGB =
                totalUploadedBytes
                        / 1024.0
                        / 1024.0
                        / 1024.0;

        System.out.println(
                "\n======================================="
        );

        System.out.println(
                "FINAL PERFORMANCE SUMMARY"
        );

        System.out.println(
                "Total Upload Count : "
                        + count
        );

        System.out.println(
                "Total Uploaded GB : "
                        + String.format("%.2f", totalGB)
        );

        System.out.println(
                "Total Execution(ms) : "
                        + totalExecution
        );

        System.out.println(
                "Average Per File(ms) : "
                        + (totalExecution / count)
        );

        System.out.println(
                "======================================="
        );
    }
}