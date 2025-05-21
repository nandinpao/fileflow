package com.agitg.airfile.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.agitg.airfile.batch.FileUploadEvent;
import com.agitg.airfile.batch.FileUploadRepository;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.controller.UploadBean;
import com.agitg.airfile.controller.UploadProgressBean;
import com.agitg.airfile.service.upload.FileStorageInfo;

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

    // ✅ 處理多儲存來源的 uploaded & complete 狀態比較，支援非同步上傳結果
    public UploadBean upload(String entryId, String fileName, String contentRange, String contentLengthStr,
            InputStream in) throws IOException, Exception {

        long contentLength = Long.parseLong(contentLengthStr);
        ChunkUpload chunk = parseChunk(contentRange, contentLengthStr);

        // ⏱️ 非同步儲存至多個服務
        Map<String, FileStorageInfo> resultPaths = strategy.saveAsynToMultiFile(entryId, fileName, in, chunk);

        // 🔁 分析每個儲存位置的狀態
        Map<String, Long> uploadedMap = new LinkedHashMap<>();
        for (Map.Entry<String, FileStorageInfo> entry : resultPaths.entrySet()) {
            String type = entry.getKey();
            FileStorageInfo info = entry.getValue();
            long uploaded = strategy.totalUploadedSize(info.getChildPath());
            uploadedMap.put(type, uploaded);
        }

        // 🎯 決定是否全部完成（所有上傳都成功）
        boolean allComplete = uploadedMap.values().stream().allMatch(u -> u == contentLength);
        long minUploaded = uploadedMap.values().stream().min(Long::compareTo).orElse(0L);

        // ✅ 儲存主檔（以第一個成功者為主）
        FileStorageInfo primary = resultPaths.values().stream().findFirst().orElseThrow();
        FileUploadEvent event = new FileUploadEvent();
        event.setFilePath(primary.getChildPath());
        event.setFileName(fileName);
        event.setProcessed(false);
        fileUploadRepository.save(event);

        // 🧾 包裝結果
        return new UploadBean()
                .setUploaded(minUploaded)
                .setComplete(allComplete)
                .setLocations(resultPaths);
    }

    public UploadBean uploadFile(String entityId, String fileId, MultipartFile file) throws IOException, Exception {

        return upload(entityId, file.getOriginalFilename(), String.valueOf(0),
                String.valueOf(file.getSize()),
                file.getInputStream());

    }

    public UploadProgressAggregate getUploadProgress(String fileId) {

        return uploadProgressService.getTotalProgress(fileId);

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

    private String getExtension(String filename) {
        int i = filename.lastIndexOf(".");
        return (i >= 0) ? filename.substring(i + 1) : null;
    }

}
