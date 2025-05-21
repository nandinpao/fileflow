package com.agitg.airfile.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.service.upload.FileStorageInfo;
import com.agitg.airfile.service.upload.StorageService;

@Service
public class StorageStrategy {

    @Autowired
    private List<StorageService> storageService;

    public Map<String, FileStorageInfo> saveToAll(String entryId, String fileName, InputStream inputStream,
            ChunkUpload chunk)
            throws IOException, Exception {

        ByteArrayOutputStream cached = new ByteArrayOutputStream();
        inputStream.transferTo(cached);
        byte[] bytes = cached.toByteArray();

        Map<String, FileStorageInfo> result = new LinkedHashMap<>();

        for (StorageService service : storageService) {
            if (!service.isEnabled())
                continue;
            try (InputStream clone = new ByteArrayInputStream(bytes)) {
                FileStorageInfo path = service.save(entryId, fileName, clone, chunk);
                result.put(service.getStorageType(), path);
            }
        }

        return result;
    }

    // 多工版：使用 CompletableFuture 執行儲存任務，加快多儲存服務處理速度
    public Map<String, FileStorageInfo> saveAsynToMultiFile(String entryId, String fileName, InputStream inputStream,
            ChunkUpload chunk) throws IOException, Exception {

        Path tempFile = Files.createTempFile("upload-buffer", null);
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            inputStream.transferTo(out);
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, storageService.size()));
        List<CompletableFuture<Map.Entry<String, FileStorageInfo>>> tasks = new ArrayList<>();

        for (StorageService service : storageService) {
            if (!service.isEnabled())
                continue;

            CompletableFuture<Map.Entry<String, FileStorageInfo>> task = CompletableFuture.supplyAsync(() -> {
                try (InputStream clone = Files.newInputStream(tempFile)) {
                    FileStorageInfo info = service.save(entryId, fileName, clone, chunk);
                    return Map.entry(service.getStorageType(), info);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor);

            tasks.add(task);
        }

        Map<String, FileStorageInfo> result = tasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        executor.shutdown();
        Files.deleteIfExists(tempFile);

        return result;
    }

    public Map<String, FileStorageInfo> saveToMultiFile(String entryId, String fileName, InputStream inputStream,
            ChunkUpload chunk)
            throws IOException, Exception {

        // 建立暫存檔案（用於複製多次 InputStream）
        Path tempFile = Files.createTempFile("upload-buffer", null);
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            inputStream.transferTo(out);
        }

        Map<String, FileStorageInfo> result = new LinkedHashMap<>();

        for (StorageService service : storageService) {
            if (!service.isEnabled())
                continue;

            try (InputStream clone = Files.newInputStream(tempFile)) {
                FileStorageInfo path = service.save(entryId, fileName, clone, chunk);
                result.put(service.getStorageType(), path);
            }
        }

        // 清除暫存檔案
        Files.deleteIfExists(tempFile);

        return result;
    }

    public long totalUploadedSize(String primaryPath) throws IOException {
        for (StorageService s : storageService) {
            if (s.isEnabled()) {
                long size = s.size(primaryPath);
                if (size > 0)
                    return size;
            }
        }
        return -1;
    }
}
