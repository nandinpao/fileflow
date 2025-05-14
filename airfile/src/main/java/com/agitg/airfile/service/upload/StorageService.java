package com.agitg.airfile.service;

import java.io.IOException;
import java.io.InputStream;

import com.agitg.airfile.controller.ChunkUpload;

public interface StorageService {

    boolean isEnabled();

    String getStorageType();

    String save(String entryId, String fileName, InputStream inputStream, ChunkUpload chunk) throws IOException;

    long size(String path) throws IOException;

}
