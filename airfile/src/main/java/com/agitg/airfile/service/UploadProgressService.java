package com.agitg.airfile.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.agitg.airfile.controller.UploadProgressBean;
import com.agitg.redisson.config.RedissonAccess;

@Component
public class UploadProgressService {

    @Autowired
    private RedissonAccess redissonAccess;

    // public void saveProgressToRedis(String type, String fileId, long uploaded,
    // long total) {

    // UploadProgressBean progressState = new UploadProgressBean(type, fileId,
    // total, uploaded, uploaded == total);

    // this.redissonAccess.setBucket(fileId, progressState,
    // Duration.ofHours(2).toMillis());

    // }

    // public UploadProgressBean getUploadProgress(String fileId) {

    // UploadProgressBean pg = this.redissonAccess.getBucketValue(fileId,
    // UploadProgressBean.class);

    // return pg;

    // }

    // ✅ 改為使用 Redis Map 結構儲存多種上傳方式進度（Hash/Map 實作）
    public void saveProgressToRedis(String type, String fileId, long uploaded, long total) {
        String redisKey = String.format("upload:progress:%s", fileId);
        UploadProgressBean progressState = new UploadProgressBean(type, fileId, total, uploaded, uploaded == total);
        this.redissonAccess.putMapValue(redisKey, type, progressState, Duration.ofHours(2).toMillis());

    }

    public UploadProgressBean getUploadProgress(String type, String fileId) {
        String redisKey = String.format("upload:progress:%s", fileId);
        return this.redissonAccess.getFromMap(redisKey, type, UploadProgressBean.class);
    }

    // 🔁 查詢所有儲存類型進度 + 計算總體進度（取 min or average）
    public UploadProgressAggregate getTotalProgress(String fileId) {

        String redisKey = String.format("upload:progress:%s", fileId);
        Map<String, UploadProgressBean> all = this.redissonAccess.getMap(redisKey);

        if (all.isEmpty()) {
            return new UploadProgressAggregate((long) 0, (long) 0, false);
        }

        long total = all.values().stream().mapToLong(UploadProgressBean::getTotal).max().orElse(0);
        Double uploaded = all.values().stream().mapToLong(UploadProgressBean::getUploaded).average().orElse(0);
        boolean complete = all.values().stream().allMatch(UploadProgressBean::isComplete);

        return new UploadProgressAggregate(total, uploaded.longValue(), complete);
    }
}
