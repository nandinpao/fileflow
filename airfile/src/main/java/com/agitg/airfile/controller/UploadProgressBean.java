package com.agitg.airfile.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UploadProgressBean {

    private String type;
    private String fileId;
    private long total;;
    private Long uploaded;
    private boolean complete;

}
