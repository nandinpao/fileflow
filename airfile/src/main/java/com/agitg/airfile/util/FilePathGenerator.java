package com.agitg.airfile.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FilePathGenerator {

    private FilePathGenerator() {
    }

    private static class Holder {
        private static final FilePathGenerator INSTANCE = new FilePathGenerator();
    }

    public static FilePathGenerator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 產生實體路徑：/scope/entity/yyyy/MM/dd/h1/h2/uuid.ext
     */
    public Path generateLocalPath(String basePath, String entityId, String originalFilename) {
        String ext = getExtension(originalFilename);

        UUID uuid = UUID.randomUUID();

        String filename = uuid + (ext != null ? "." + ext : "");
        String[] date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")).split("/");
        String h1 = uuid.toString().substring(0, 2);
        String h2 = uuid.toString().substring(2, 4);

        return Paths.get(basePath, entityId, date[0], date[1], date[2], h1, h2, filename);
    }

    /**
     * 產生 MinIO 物件 key：與本地一致格式，但為 S3 路徑字串
     */
    public String generateMinioObjectKey(String basePath, String entityId,
            String originalFilename) {
        Path path = generateLocalPath(basePath, entityId, originalFilename);
        return path.toString().replace(File.separator, "/");
    }

    /**
     * 建立對應的本地資料夾並儲存檔案
     */
    public Path saveToLocal(Path relativePath, InputStream in) throws IOException {
        Path target = relativePath;
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf(".");
        return (i >= 0 && i < name.length() - 1) ? name.substring(i + 1) : null;
    }

}
