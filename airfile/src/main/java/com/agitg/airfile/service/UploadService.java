package com.agitg.airfile.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.batch.FileUploadEvent;
import com.agitg.airfile.batch.FileUploadRepository;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.controller.UploadBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UploadService {

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private StorageStrategy strategy;

    @Autowired
    private UploadProgressService uploadProgressService;

    public UploadBean upload(String entryId, String fileName, String contentRange, String contentLengthStr,
            InputStream in)
            throws IOException {

        long contentLength = Long.parseLong(contentLengthStr);
        ChunkUpload chunk = parseChunk(contentRange, contentLengthStr);

        // String entryId = UUID.randomUUID().toString(); // to implement from a
        // authorized user id

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

    public UploadBean getUploadProgress(String fileId) {

        return uploadProgressService.getUploadProgress(fileId);

    }

}
