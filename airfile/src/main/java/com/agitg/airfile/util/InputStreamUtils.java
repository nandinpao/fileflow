package com.agitg.airfile.util;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtils {

    private InputStreamUtils() {
    }

    private static class Holder {
        private static final InputStreamUtils INSTANCE = new InputStreamUtils();
    }

    public static InputStreamUtils getInstance() {
        return Holder.INSTANCE;
    }

    public long getInputStreamSize(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytes = 0;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalBytes += bytesRead;
        }
        return totalBytes;
    }

}
