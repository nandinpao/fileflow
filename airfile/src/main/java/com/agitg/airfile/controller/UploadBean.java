package com.agitg.airfile.controller;

import java.util.List;
import java.util.Map;

import com.agitg.airfile.service.upload.FileStorageInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UploadBean {

    private String fileId;
    private long total;;
    private Long uploaded;
    private Boolean complete;

    private String path;
    private String type;

    private Map<String, FileStorageInfo> locations;
}
