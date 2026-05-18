package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.SysOperationLog;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.SysOperationLogMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统操作日志服务
 */
@Service
public class SysOperationLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(SysOperationLogService.class);
    
    @Resource
    private SysOperationLogMapper sysOperationLogMapper;
    
    @Resource
    private ObjectMapper objectMapper;
    
    /**
     * 异步保存操作日志
     */
    @Async
    public void saveLogAsync(SysOperationLog log) {
        try {
            // 设置创建时间
            if (log.getCreateTime() == null) {
                log.setCreateTime(LocalDateTime.now());
            }
            sysOperationLogMapper.insert(log);
            logger.info("操作日志保存成功: 用户={}, 类型={}, 描述={}, IP={}", 
                    log.getUsername(), log.getOperationType(), log.getOperationDesc(), log.getIpAddress());
        } catch (Exception e) {
            logger.error("保存操作日志失败", e);
        }
    }
    
    /**
     * 同步保存操作日志
     */
    public void saveLog(SysOperationLog log) {
        try {
            if (log.getCreateTime() == null) {
                log.setCreateTime(LocalDateTime.now());
            }
            sysOperationLogMapper.insert(log);
        } catch (Exception e) {
            logger.error("保存操作日志失败", e);
        }
    }
    
    /**
     * 分页查询日志
     */
    public Page<SysOperationLog> getLogsByPage(String username, String operationType, 
            String startTime, String endTime, Integer currentPage, Integer size) {
        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.isNotBlank(operationType)) {
            queryWrapper.eq(SysOperationLog::getOperationType, operationType);
        }
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge(SysOperationLog::getCreateTime, startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(SysOperationLog::getCreateTime, endTime);
        }
        
        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);
        
        return sysOperationLogMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 获取所有日志（用于导出）
     */
    public List<SysOperationLog> getAllLogs(String username, String operationType, 
            String startTime, String endTime) {
        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.isNotBlank(operationType)) {
            queryWrapper.eq(SysOperationLog::getOperationType, operationType);
        }
        if (StringUtils.isNotBlank(startTime)) {
            queryWrapper.ge(SysOperationLog::getCreateTime, startTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(SysOperationLog::getCreateTime, endTime);
        }
        
        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);
        
        return sysOperationLogMapper.selectList(queryWrapper);
    }
    
    /**
     * 根据ID删除日志
     */
    public void deleteById(Long id) {
        sysOperationLogMapper.deleteById(id);
    }
    
    /**
     * 批量删除日志
     */
    public void deleteBatch(List<Long> ids) {
        sysOperationLogMapper.deleteBatchIds(ids);
    }
    
    /**
     * 获取当前请求的IP地址
     */
    public String getClientIp() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip;
            }
        } catch (Exception e) {
            logger.error("获取客户端IP异常", e);
        }
        return "unknown";
    }
    
    /**
     * 获取当前请求的端口
     */
    public int getClientPort() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getRemotePort();
            }
        } catch (Exception e) {
            logger.error("获取客户端端口异常", e);
        }
        return 0;
    }
    
    /**
     * 获取当前请求的UserAgent
     */
    public String getUserAgent() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            logger.error("获取UserAgent异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前请求的完整URL
     */
    public String getRequestUrl() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return request.getRequestURI();
            }
        } catch (Exception e) {
            logger.error("获取请求URL异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前请求的方法
     */
    public String getRequestMethod() {
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getMethod();
            }
        } catch (Exception e) {
            logger.error("获取请求方法异常", e);
        }
        return "";
    }
    
    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        try {
            return JwtTokenUtils.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
