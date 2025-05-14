package com.agitg.airfile.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.agitg.airfile.controller.UploadBean;
import com.agitg.redisson.config.RedissonAccess;

@Component
public class UploadProgressService {

    @Autowired
    private RedissonAccess redissonAccess;

    public void saveProgressToRedis(String fileId, long uploaded, long total) {

        UploadBean progressState = new UploadBean(fileId, total, uploaded, uploaded == total);

        this.redissonAccess.setBucket(fileId, progressState, Duration.ofHours(2).toMillis());

    }

    public UploadBean getUploadProgress(String fileId) {

        UploadBean pg = this.redissonAccess.getBucketValue(fileId, UploadBean.class);

        return pg;
    }
}
