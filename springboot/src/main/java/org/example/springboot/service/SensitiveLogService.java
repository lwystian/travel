package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.SensitiveLog;

public interface SensitiveLogService extends IService<SensitiveLog> {
    void saveSensitiveLog(SensitiveLog log);
    void recordSensitiveCheck(Long userId, String username, String content, String contentType,
                              String objectId, String detectedWords, Integer detectedCount,
                              String handleType, String handleResult, String ip, String userAgent);
}
