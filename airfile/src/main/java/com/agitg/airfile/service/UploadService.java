package com.agitg.airfile.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.batch.FileUploadEvent;
import com.agitg.airfile.batch.FileUploadRepository;
import com.agitg.airfile.config.StorageProperties;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.controller.UploadBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UploadService {

    @Autowired
    private StorageProperties properties;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private StorageStrategy strategy;

    public UploadBean upload(String fileName, String contentRange, String contentLengthStr,
            InputStream in)
            throws IOException {

        long contentLength = Long.parseLong(contentLengthStr);
        ChunkUpload chunk = parseChunk(contentRange, contentLengthStr);

        String entryId = ""; // need to implement

        Map<String, String> resultPaths = strategy.saveToAll(entryId, fileName, in, chunk);

        // 儲存主要一個位置的狀態
        String primaryPath = resultPaths.values().stream().findFirst().orElseThrow();
        long uploaded = strategy.totalUploadedSize(primaryPath);
        boolean complete = uploaded == contentLength;

        FileUploadEvent event = new FileUploadEvent();
        event.setFilePath(primaryPath);
        event.setFileName(fileName);
        event.setProcessed(false);
        fileUploadRepository.save(event);

        return new UploadBean().setUploaded(uploaded).setComplete(complete);
    }

    private ChunkUpload parseChunk(String contentRange, String fallbackLength) {

        if (contentRange == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)").matcher(contentRange);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Content-Range");
        }
        return new ChunkUpload(
                Long.parseLong(matcher.group(1)),
                Long.parseLong(matcher.group(2)),
                Long.parseLong(matcher.group(3)));
    }

    public long getInputStreamSize(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytes = 0;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalBytes += bytesRead;
        }
        return totalBytes;
    }

    public UploadBean getCurrentStatus(String fileId) {

        File file = new File(properties.getStorage().getLocalFile().getPath() + fileId);
        long size = file.exists() ? file.length() : 0;

        return new UploadBean().setUploaded(size);
    }

    public UploadBean getFileStatus(String fileId) throws IOException {

        Path filePath = Paths.get(properties.getStorage().getLocalFile().getPath()).resolve(fileId);
        long uploadedSize = Files.exists(filePath) ? Files.size(filePath) : 0;

        return new UploadBean().setUploaded(uploadedSize);

    }
}
