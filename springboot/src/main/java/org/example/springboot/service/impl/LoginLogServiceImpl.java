package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.LoginLog;
import org.example.springboot.mapper.LoginLogMapper;
import org.example.springboot.service.LoginLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Override
    @Async
    public void saveLoginLog(LoginLog log) {
        this.save(log);
    }

    @Override
    @Async
    public void recordLogin(Long userId, String username, String nickname, String loginType, String loginStatus,
                            String failReason, String ip, Integer port, String userAgent, String deviceType,
                            String browser, String os, String location) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setNickname(nickname);
        log.setLoginType(loginType);
        log.setLoginStatus(loginStatus);
        log.setFailReason(failReason);
        log.setIp(ip);
        log.setPort(port);
        log.setUserAgent(userAgent);
        log.setDeviceType(deviceType);
        log.setBrowser(browser);
        log.setOs(os);
        log.setLocation(location);
        log.setCreateTime(LocalDateTime.now());
        log.setStatus(1);
        this.save(log);
    }

    @Override
    public List<LoginLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), LoginLog::getUsername, username);
        wrapper.ge(startTime != null, LoginLog::getCreateTime, startTime);
        wrapper.le(endTime != null, LoginLog::getCreateTime, endTime);
        wrapper.orderByDesc(LoginLog::getCreateTime);
        wrapper.last("LIMIT " + ((page - 1) * limit) + ", " + limit);
        return this.list(wrapper);
    }

    @Override
    public long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), LoginLog::getUsername, username);
        wrapper.ge(startTime != null, LoginLog::getCreateTime, startTime);
        wrapper.le(endTime != null, LoginLog::getCreateTime, endTime);
        return this.count(wrapper);
    }
}
