package com.agitg.airfile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@ConfigurationProperties(prefix = "airfile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageProperties {

    private Config config;
    private Schedule schedule;
    private Storage storage;

    @Data
    public static class Config {
        private long fileThresholdBytes;
    }

    @Data
    public static class Schedule {
        private FileType fileType;

        @Data
        public static class FileType {
            private long interval;
        }
    }

    @Data
    public static class Storage {
        private Minio minio;
        private LocalFile localFile;

        @Data
        public static class Minio {
            private boolean enable;
            private String domain;
            private String url;
            private String accessKey;
            private String secretKey;
            private String bucket;
        }

        @Data
        public static class LocalFile {
            private boolean enable;
            private String path;
            private String domain;
        }
    }
}
