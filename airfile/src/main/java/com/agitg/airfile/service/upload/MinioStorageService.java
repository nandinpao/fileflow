package com.agitg.airfile.service.upload;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agitg.airfile.config.StorageProperties;
import com.agitg.airfile.controller.ChunkUpload;
import com.agitg.airfile.service.StorageService;
import com.agitg.airfile.service.UploadProgressService;
import com.agitg.airfile.stream.ProgressInputStream;
import com.agitg.airfile.util.InputStreamUtils;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;

@Service
public class MinioStorageService implements StorageService {

    @Autowired
    private StorageProperties properties;

    @Autowired
    private UploadProgressService uploadProgressService;

    @PostConstruct
    public void init() throws Exception {
        if (!properties.getStorage().getMinio().isEnable())
            return;

        MinioClient client = getClient();
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getStorage().getMinio().getBucket()).build());

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getStorage().getMinio().getBucket()).build());
        }
    }

    @Override
    public String save(String entryId, String fileName, InputStream inputStream, ChunkUpload chunk) throws IOException {
        try {

            long total = InputStreamUtils.getInstance().getInputStreamSize(inputStream);

            InputStream progressStream = new ProgressInputStream(
                    inputStream, total, entryId, uploadProgressService);

            MinioClient client = getClient();
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getStorage().getMinio().getBucket())
                    .object(fileName)
                    .stream(progressStream, total, -1)
                    .contentType("application/octet-stream")
                    .build());

            return fileName;

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public long size(String path) {
        // Optional: MinIO doesn't support file size in same way. Skip for chunked.
        return -1;
    }

    private MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(properties.getStorage().getMinio().getUrl())
                .credentials(properties.getStorage().getMinio().getAccessKey(),
                        properties.getStorage().getMinio().getSecretKey())
                .build();
    }

    @Override
    public boolean isEnabled() {
        return this.properties.getStorage().getMinio().isEnable();
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
}
