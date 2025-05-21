package com.agitg.airfile.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.agitg.airfile.service.UploadProgressAggregate;
import com.agitg.airfile.service.UploadService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

@RestController
@RequestMapping("/api/upload")
public class UploadResumeController {

    @Autowired
    private UploadService uploadService;

    @PostMapping(value = "/resume", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<UploadBean> uploadWithOffset(
            HttpServletRequest request,
            @RequestParam("entryId") String entryId,
            @RequestParam("fileId") String fileId,
            @RequestHeader("Content-Range") String contentRange,
            @RequestHeader("Content-Length") String contentLength) throws IOException, Exception {

        return ResponseEntity
                .ok(uploadService.upload(entryId, fileId, contentRange, contentLength, request.getInputStream()));
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadBean> upload(HttpServletRequest request,
            @RequestHeader("Content-Range") String contentRange,
            @RequestHeader("Content-Length") String contentLength) throws Exception {

        UploadRequestDto meta = null;
        InputStream fileStream = null;
        String fileName = null;

        for (Part part : request.getParts()) {
            switch (part.getName()) {
                case "meta" -> {
                    try (InputStream in = part.getInputStream()) {
                        meta = new ObjectMapper().readValue(in, UploadRequestDto.class);
                    }
                }
                case "file" -> {
                    fileStream = part.getInputStream();
                    fileName = part.getSubmittedFileName(); // 原始檔名
                }
            }
        }

        if (meta == null || fileStream == null) {
            throw new IllegalArgumentException("缺少 meta 或 file 欄位");
        }

        return ResponseEntity.ok(
                uploadService.upload(
                        meta.getEntryId(),
                        fileName,
                        contentRange,
                        contentLength,
                        fileStream));
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadBean> uploadMultipart(
            @RequestPart("meta") UploadRequestDto meta,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Content-Range") String contentRange,
            @RequestHeader("Content-Length") String contentLength) throws Exception {

        return ResponseEntity.ok(
                uploadService.upload(
                        meta.getEntryId(), meta.getFileId(),
                        contentRange, contentLength,
                        file.getInputStream()));
    }

    @PostMapping("/chunk")
    public ResponseEntity<UploadBean> uploadChunk(
            @RequestParam("entryId") String entryId,
            @RequestParam("fileId") String fileId,
            @RequestParam("file") MultipartFile file) throws IOException, Exception {

        return ResponseEntity
                .ok(uploadService.uploadFile(entryId, fileId, file));
    }

    @GetMapping(value = "/resume/status")
    public ResponseEntity<UploadProgressAggregate> getUploadedSize(@RequestParam("fileId") String fileId) {
        return ResponseEntity.ok(uploadService.getUploadProgress(fileId));
    }

}
