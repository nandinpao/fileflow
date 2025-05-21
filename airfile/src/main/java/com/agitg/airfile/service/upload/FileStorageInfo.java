package com.agitg.airfile.service.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileStorageInfo {

    private String parentPath;

    private String childPath;

    private String domain;

}
