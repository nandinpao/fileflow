package com.agitg.airfile.service.upload;

import java.io.IOException;
import java.io.InputStream;

import com.agitg.airfile.controller.ChunkUpload;

public interface StorageService {

    boolean isEnabled();

    String getStorageType();

    FileStorageInfo save(String entryId, String fileName, InputStream inputStream, ChunkUpload chunk)
            throws IOException, Exception;

    long size(String path) throws IOException;

}
