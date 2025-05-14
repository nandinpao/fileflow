package com.agitg.airfile.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.service.upload.StorageService;

@Service
public class StorageStrategy {

    @Autowired
    private List<StorageService> services;

    public Map<String, String> saveToAll(String entryId, String fileName, InputStream inputStream, ChunkUpload chunk)
            throws IOException {
        ByteArrayOutputStream cached = new ByteArrayOutputStream();
        inputStream.transferTo(cached);
        byte[] bytes = cached.toByteArray();

        Map<String, String> result = new LinkedHashMap<>();

        for (StorageService service : services) {
            if (!service.isEnabled())
                continue;
            try (InputStream clone = new ByteArrayInputStream(bytes)) {
                String path = service.save(entryId, fileName, clone, chunk);
                result.put(service.getStorageType(), path);
            }
        }

        return result;
    }

    public long totalUploadedSize(String primaryPath) throws IOException {
        for (StorageService s : services) {
            if (s.isEnabled()) {
                long size = s.size(primaryPath);
                if (size > 0)
                    return size;
            }
        }
        return -1;
    }
}
