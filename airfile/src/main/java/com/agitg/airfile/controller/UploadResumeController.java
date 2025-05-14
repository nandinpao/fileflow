package com.agitg.airfile.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agitg.airfile.service.UploadService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/upload")
public class UploadResumeController {

    @Autowired
    private UploadService uploadService;

    @PostMapping(value = "/resume")
    public ResponseEntity<?> uploadWithOffset(
            HttpServletRequest request,
            @RequestParam("fileId") String fileId,
            @RequestHeader("Content-Range") String contentRange,
            @RequestHeader("Content-Length") String contentLength) throws IOException {

        return ResponseEntity.ok(uploadService.upload(fileId, contentRange, contentLength, request.getInputStream()));
    }

    @GetMapping(value = "/resume/status")
    public ResponseEntity<?> getUploadedSize(@RequestParam("fileId") String fileId) {
        return ResponseEntity.ok(uploadService.getUploadProgress(fileId));
    }

}
