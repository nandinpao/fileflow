package com.agitg.airfile.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UploadProgressAggregate {

    private Long total;;
    private Long uploaded;
    private Boolean complete;
}
