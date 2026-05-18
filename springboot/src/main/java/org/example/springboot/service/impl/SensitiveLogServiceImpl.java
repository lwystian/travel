package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.SensitiveLog;
import org.example.springboot.mapper.SensitiveLogMapper;
import org.example.springboot.service.SensitiveLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class SensitiveLogServiceImpl extends ServiceImpl<SensitiveLogMapper, SensitiveLog> implements SensitiveLogService {

    @Override
    @Async
    public void saveSensitiveLog(SensitiveLog log) {
        this.save(log);
    }

    @Override
    @Async
    public void recordSensitiveCheck(Long userId, String username, String content, String contentType,
                                     String objectId, String detectedWords, Integer detectedCount,
                                     String handleType, String handleResult, String ip, String userAgent) {
        SensitiveLog log = new SensitiveLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setContent(content);
        log.setContentType(contentType);
        log.setObjectId(objectId);
        log.setDetectedWords(detectedWords);
        log.setDetectedCount(detectedCount);
        log.setHandleType(handleType);
        log.setHandleResult(handleResult);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setCreateTime(LocalDateTime.now());
        log.setStatus(1);
        this.save(log);
    }
}
