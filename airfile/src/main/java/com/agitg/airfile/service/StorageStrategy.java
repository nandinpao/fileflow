package com.agitg.airfile.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

import lombok.extern.slf4j.Slf4j;

/**
 * @author nandin.pao
 */
@Slf4j
@Service
public class StorageStrategy {

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private List<StorageService> storageService;

    // ✅ 根據檔案大小自動切換小檔記憶體模式 vs 大檔暫存檔案模式
    public Map<String, FileStorageInfo> saveAsynToMultiFile(String entryId, String fileName, InputStream inputStream,
            ChunkUpload chunk) throws IOException, Exception {

        final long SIZE_THRESHOLD = storageProperties.getConfig().getFileThresholdBytes();
        final long MAX_SAFE_THRESHOLD = Runtime.getRuntime().maxMemory() / 4;

        if (SIZE_THRESHOLD > MAX_SAFE_THRESHOLD) {
            throw new IllegalStateException(
                    "The configured file threshold is too large, which may lead to memory overflow. Please adjust it to a safe value (recommended ≦"
                            + MAX_SAFE_THRESHOLD + ")");
        }

        ByteArrayOutputStream smallBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long totalRead = 0;
        boolean exceeded = false;

        // 緩衝前段資料 + 判斷是否超過閾值
        while ((read = inputStream.read(buffer)) != -1) {
            smallBuffer.write(buffer, 0, read);
            totalRead += read;
            if (totalRead > SIZE_THRESHOLD) {
                exceeded = true;
                break;
            }
        }

        InputStream combinedStream = exceeded
                ? new SequenceInputStream(new ByteArrayInputStream(smallBuffer.toByteArray()), inputStream)
                : new ByteArrayInputStream(smallBuffer.toByteArray());

        Map<String, FileStorageInfo> result;

        if (!exceeded) {
            result = processInMemory(entryId, fileName, smallBuffer.toByteArray(), chunk);
        } else {
            // 🚨 警告 log（非例外）
            log.warn(
                    "[upload] Large file upload detected ({} bytes), will use temp file strategy, entryId={}, fileName={}",
                    totalRead, entryId, fileName);

            Path tempFile = Files.createTempFile("upload-buffer", null);

            // ⚠️ 確認磁碟空間充足
            FileStore store = Files.getFileStore(tempFile);
            long usableSpace = store.getUsableSpace();

            if (usableSpace < totalRead * 2) {
                throw new IOException("Not enough disk space to process the upload temporary file (at least"
                        + (totalRead * 2 / 1024 / 1024) + " MB required）");
            }

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
