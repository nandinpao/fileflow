package com.agitg.airfile.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.agitg.airfile.service.UploadProgressService;

public class ProgressInputStream extends FilterInputStream {

    private final long totalBytes;
    private long uploadedBytes = 0;
    private final String fileId;
    private final UploadProgressService progressService;
    private String type;

    public ProgressInputStream(InputStream in, long totalBytes,
            String fileId, String type, UploadProgressService progressService) {
        super(in);
        this.totalBytes = totalBytes;
        this.fileId = fileId;
        this.type = type;
        this.progressService = progressService;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) {
            uploadedBytes += count;
            progressService.saveProgressToRedis(type, fileId, uploadedBytes, totalBytes);
        }
        return count;
    }
}
