package com.agitg.airfile.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.config.StorageProperties;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.util.FilePathGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Autowired
    private StorageProperties properties;

    @Override
    public String save(String entryId, String fileName, InputStream inputStream, ChunkUpload chunk) throws IOException {

        File dir = new File(properties.getStorage().getLocalFile().getPath());
        if (!dir.exists())
            dir.mkdirs();

        // File target = new File(dir, fileId);

        Path path = FilePathGenerator.getInstance().generateLocalPath(properties.getStorage().getLocalFile().getPath(),
                fileName,
                fileName);

        File target = path.toFile();

        if (chunk == null) {
            try (FileOutputStream out = new FileOutputStream(target)) {
                inputStream.transferTo(out);
            }
        } else {
            try (RandomAccessFile raf = new RandomAccessFile(target, "rw")) {
                raf.seek(chunk.getStart());
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    raf.write(buffer, 0, read);
                }
            }
        }
        return target.getAbsolutePath();
    }

    @Override
    public long size(String path) throws IOException {
        return new File(path).length();
    }

    @Override
    public boolean isEnabled() {
        return this.properties.getStorage().getLocalFile().isEnable();
    }

    @Override
    public String getStorageType() {
        return "local-file";
    }
}
