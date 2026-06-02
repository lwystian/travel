package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.SysLog;
import org.example.springboot.mapper.SysLogMapper;
import org.example.springboot.security.SecurityValidationUtil;
import org.example.springboot.service.SysLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    @Async
    public void saveLog(SysLog log) {
        this.save(log);
    }

    @Override
    @Async
    public void saveLog(Long userId, String username, String nickname, String operation, String operationName,
                        String objectType, String objectId, String objectName, String method, String requestUrl,
                        String requestParams, String ip, Integer port, String userAgent, String result,
                        String errorMsg, Integer duration) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setNickname(nickname);
        log.setOperation(operation);
        log.setOperationName(operationName);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setObjectName(objectName);
        log.setMethod(method);
        log.setRequestUrl(requestUrl);
        log.setRequestParams(requestParams);
        log.setIp(ip);
        log.setPort(port);
        log.setUserAgent(userAgent);
        log.setResult(result);
        log.setErrorMsg(errorMsg);
        log.setDuration(duration);
        log.setCreateTime(LocalDateTime.now());
        log.setStatus(1);
        this.save(log);
    }

    @Override
    public void deleteBeforeTime(LocalDateTime time) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysLog::getCreateTime, time);
        this.remove(wrapper);
    }

    @Override
    public List<SysLog> queryLogs(int page, int limit, String username, LocalDateTime startTime, LocalDateTime endTime) {
        page = SecurityValidationUtil.clampPage(page);
        limit = SecurityValidationUtil.clampLimit(limit, 10, 100);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), SysLog::getUsername, username);
        wrapper.ge(startTime != null, SysLog::getCreateTime, startTime);
        wrapper.le(endTime != null, SysLog::getCreateTime, endTime);
        wrapper.orderByDesc(SysLog::getCreateTime);
        wrapper.last("LIMIT " + ((page - 1) * limit) + ", " + limit);
        return this.list(wrapper);
    }

    @Override
    public long countLogs(String username, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), SysLog::getUsername, username);
        wrapper.ge(startTime != null, SysLog::getCreateTime, startTime);
        wrapper.le(endTime != null, SysLog::getCreateTime, endTime);
        return this.count(wrapper);
    }
}
