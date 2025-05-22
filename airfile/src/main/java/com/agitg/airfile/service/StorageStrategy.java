package com.agitg.airfile.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
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

import com.agitg.airfile.config.StorageProperties;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.service.upload.FileStorageInfo;
import com.agitg.airfile.service.upload.StorageService;

/**
 * @author nandin.pao
 */
@Service
public class StorageStrategy {

    @Autowired
    private StorageProperties storageProperties;

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

    // ✅ 根據檔案大小自動切換小檔記憶體模式 vs 大檔暫存檔案模式
    public Map<String, FileStorageInfo> saveAsynToMultiFile(String entryId, String fileName, InputStream inputStream,
            ChunkUpload chunk) throws IOException, Exception {

        final long SIZE_THRESHOLD = storageProperties.getConfig().getFileThresholdBytes();
        ByteArrayOutputStream smallBuffer = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int read;
        long totalRead = 0;
        boolean exceeded = false;

        while ((read = inputStream.read(buffer)) != -1) {
            smallBuffer.write(buffer, 0, read);
            totalRead += read;
            if (totalRead > SIZE_THRESHOLD) {
                exceeded = true;
                break;
            }
        }

        InputStream combinedStream = exceeded
                ? new SequenceInputStream(
                        new ByteArrayInputStream(smallBuffer.toByteArray()),
                        inputStream)
                : new ByteArrayInputStream(smallBuffer.toByteArray());

        Map<String, FileStorageInfo> result;

        if (!exceeded) {
            // ✅ 小檔：記憶體中處理
            result = processInMemory(entryId, fileName, smallBuffer.toByteArray(), chunk);
        } else {
            // ✅ 大檔：寫入暫存檔再多工處理
            Path tempFile = Files.createTempFile("upload-buffer", null);
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                combinedStream.transferTo(out);
            }
            result = processWithTempFile(entryId, fileName, tempFile, chunk);
            Files.deleteIfExists(tempFile);
        }

        return result;
    }

    private Map<String, FileStorageInfo> processInMemory(String entryId, String fileName, byte[] bytes,
            ChunkUpload chunk) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, storageService.size()));
        List<CompletableFuture<Map.Entry<String, FileStorageInfo>>> tasks = new ArrayList<>();

        for (StorageService service : storageService) {
            if (!service.isEnabled())
                continue;

            CompletableFuture<Map.Entry<String, FileStorageInfo>> task = CompletableFuture.supplyAsync(() -> {
                try (InputStream clone = new ByteArrayInputStream(bytes)) {
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
        return result;
    }

    private Map<String, FileStorageInfo> processWithTempFile(String entryId, String fileName, Path tempFile,
            ChunkUpload chunk)
            throws IOException {
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
