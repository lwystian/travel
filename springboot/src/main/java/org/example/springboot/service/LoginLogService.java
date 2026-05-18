package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.LoginLog;
import java.time.LocalDateTime;
import java.util.List;

public interface LoginLogService extends IService<LoginLog> {
    void saveLoginLog(LoginLog log);
    void recordLogin(Long userId, String username, String nickname, String loginType, String loginStatus,
                     String failReason, String ip, Integer port, String userAgent, String deviceType,
                     String browser, String os, String location);
    List<LoginLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime);
    long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime);
}
