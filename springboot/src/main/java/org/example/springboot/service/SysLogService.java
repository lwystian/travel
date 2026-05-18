package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.SysLog;
import java.time.LocalDateTime;
import java.util.List;

public interface SysLogService extends IService<SysLog> {
    void saveLog(SysLog log);
    void saveLog(Long userId, String username, String nickname, String operation, String operationName,
                 String objectType, String objectId, String objectName, String method, String requestUrl,
                 String requestParams, String ip, Integer port, String userAgent, String result,
                 String errorMsg, Integer duration);
    void deleteBeforeTime(LocalDateTime time);
    List<SysLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime);
    long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime);
}
