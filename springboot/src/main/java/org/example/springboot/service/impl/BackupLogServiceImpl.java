package org.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.BackupLogMapper;
import org.example.springboot.service.*;
import org.example.springboot.common.LogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BackupLogServiceImpl extends ServiceImpl<BackupLogMapper, BackupLog> implements BackupLogService {
    @Autowired private SysLogService sysLogService;
    @Autowired private LoginLogService loginLogService;
    @Autowired private SensitiveLogService sensitiveLogService;
    @Autowired private ReviewLogService reviewLogService;
    private static final String BACKUP_DIR = System.getProperty("user.dir") + "/backup/";

    @Override
    public Map<String, Object> backupAndExport(String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();
        BackupLog backup = new BackupLog();
        backup.setBackupType(LogType.BackupType.LOG);
        backup.setBackupTable(tableName);
        backup.setTimeRangeStart(startTime);
        backup.setTimeRangeEnd(endTime);
        backup.setStatus("RUNNING");
        this.save(backup);
        try {
            List<Map<String, Object>> data = getTableData(tableName, startTime, endTime);
            new File(BACKUP_DIR).mkdirs();
            String fileName = tableName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".json";
            File file = new File(BACKUP_DIR + fileName);
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.write(data.toString());
            }
            backup.setBackupName(fileName);
            backup.setBackupPath(file.getAbsolutePath());
            backup.setBackupSize(file.length());
            backup.setRecordCount(data.size());
            backup.setStatus("SUCCESS");
            backup.setFileUrl("/backup/" + fileName);
            backup.setCreateTime(LocalDateTime.now());
            backup.setExpireTime(LocalDateTime.now().plusMonths(6));
            backup.setStatusFlag(1);
            this.updateById(backup);
            result.put("success", true);
            result.put("fileUrl", backup.getFileUrl());
            result.put("recordCount", data.size());
        } catch (Exception e) {
            backup.setStatus("FAIL");
            backup.setErrorMsg(e.getMessage());
            this.updateById(backup);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> queryLogs(String tableName, int page, int limit, String username, String startTime, String endTime) {
        List<Object> records = new ArrayList<>();
        long total = 0;
        
        switch (tableName) {
            case "sys_log":
                QueryWrapper<SysLog> w1 = new QueryWrapper<>();
                if (username != null && !username.isEmpty()) w1.like("username", username);
                if (startTime != null && !startTime.isEmpty()) w1.ge("create_time", startTime);
                if (endTime != null && !endTime.isEmpty()) w1.le("create_time", endTime);
                w1.orderByDesc("create_time");
                List<SysLog> sysList = sysLogService.list(w1);
                records.addAll(sysList);
                total = sysList.size();
                break;
            case "login_log":
                QueryWrapper<LoginLog> w2 = new QueryWrapper<>();
                if (username != null && !username.isEmpty()) w2.like("username", username);
                if (startTime != null && !startTime.isEmpty()) w2.ge("create_time", startTime);
                if (endTime != null && !endTime.isEmpty()) w2.le("create_time", endTime);
                w2.orderByDesc("create_time");
                List<LoginLog> loginList = loginLogService.list(w2);
                records.addAll(loginList);
                total = loginList.size();
                break;
            case "sensitive_log":
                QueryWrapper<SensitiveLog> w3 = new QueryWrapper<>();
                if (username != null && !username.isEmpty()) w3.like("username", username);
                if (startTime != null && !startTime.isEmpty()) w3.ge("create_time", startTime);
                if (endTime != null && !endTime.isEmpty()) w3.le("create_time", endTime);
                w3.orderByDesc("create_time");
                List<SensitiveLog> sensitiveList = sensitiveLogService.list(w3);
                records.addAll(sensitiveList);
                total = sensitiveList.size();
                break;
            case "review_log":
                QueryWrapper<ReviewLog> w4 = new QueryWrapper<>();
                if (username != null && !username.isEmpty()) w4.like("username", username);
                if (startTime != null && !startTime.isEmpty()) w4.ge("create_time", startTime);
                if (endTime != null && !endTime.isEmpty()) w4.le("create_time", endTime);
                w4.orderByDesc("create_time");
                List<ReviewLog> reviewList = reviewLogService.list(w4);
                records.addAll(reviewList);
                total = reviewList.size();
                break;
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("records", records);
        map.put("total", total);
        map.put("pages", (total + limit - 1) / limit);
        return map;
    }

    private List<Map<String, Object>> getTableData(String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        switch (tableName) {
            case "sys_log":
                QueryWrapper<SysLog> w1 = new QueryWrapper<>();
                if (startTime != null) w1.ge("create_time", startTime);
                if (endTime != null) w1.le("create_time", endTime);
                w1.orderByDesc("create_time");
                for (SysLog log : sysLogService.list(w1)) {
                    result.add(createSysLogMap(log));
                }
                break;
            case "login_log":
                QueryWrapper<LoginLog> w2 = new QueryWrapper<>();
                if (startTime != null) w2.ge("create_time", startTime);
                if (endTime != null) w2.le("create_time", endTime);
                w2.orderByDesc("create_time");
                for (LoginLog log : loginLogService.list(w2)) {
                    result.add(createLoginLogMap(log));
                }
                break;
            case "sensitive_log":
                QueryWrapper<SensitiveLog> w3 = new QueryWrapper<>();
                if (startTime != null) w3.ge("create_time", startTime);
                if (endTime != null) w3.le("create_time", endTime);
                w3.orderByDesc("create_time");
                for (SensitiveLog log : sensitiveLogService.list(w3)) {
                    result.add(createSensitiveLogMap(log));
                }
                break;
            case "review_log":
                QueryWrapper<ReviewLog> w4 = new QueryWrapper<>();
                if (startTime != null) w4.ge("create_time", startTime);
                if (endTime != null) w4.le("create_time", endTime);
                w4.orderByDesc("create_time");
                for (ReviewLog log : reviewLogService.list(w4)) {
                    result.add(createReviewLogMap(log));
                }
                break;
        }
        return result;
    }

    private Map<String, Object> createSysLogMap(SysLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("nickname", log.getNickname());
        map.put("operation", log.getOperation());
        map.put("operationName", log.getOperationName());
        map.put("objectType", log.getObjectType());
        map.put("objectId", log.getObjectId());
        map.put("objectName", log.getObjectName());
        map.put("method", log.getMethod());
        map.put("requestUrl", log.getRequestUrl());
        map.put("ip", log.getIp());
        map.put("port", log.getPort());
        map.put("result", log.getResult());
        map.put("errorMsg", log.getErrorMsg());
        map.put("duration", log.getDuration());
        map.put("createTime", log.getCreateTime());
        return map;
    }

    private Map<String, Object> createLoginLogMap(LoginLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("nickname", log.getNickname());
        map.put("loginType", log.getLoginType());
        map.put("loginStatus", log.getLoginStatus());
        map.put("failReason", log.getFailReason());
        map.put("ip", log.getIp());
        map.put("port", log.getPort());
        map.put("deviceType", log.getDeviceType());
        map.put("browser", log.getBrowser());
        map.put("os", log.getOs());
        map.put("location", log.getLocation());
        map.put("createTime", log.getCreateTime());
        return map;
    }

    private Map<String, Object> createSensitiveLogMap(SensitiveLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("content", log.getContent());
        map.put("contentType", log.getContentType());
        map.put("objectId", log.getObjectId());
        map.put("detectedWords", log.getDetectedWords());
        map.put("detectedCount", log.getDetectedCount());
        map.put("handleType", log.getHandleType());
        map.put("handleResult", log.getHandleResult());
        map.put("ip", log.getIp());
        map.put("createTime", log.getCreateTime());
        return map;
    }

    private Map<String, Object> createReviewLogMap(ReviewLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("nickname", log.getNickname());
        map.put("targetType", log.getTargetType());
        map.put("targetId", log.getTargetId());
        map.put("targetUserId", log.getTargetUserId());
        map.put("targetSummary", log.getTargetSummary());
        map.put("contentPreview", log.getContentPreview());
        map.put("reviewType", log.getReviewType());
        map.put("reviewStatus", log.getReviewStatus());
        map.put("previousStatus", log.getPreviousStatus());
        map.put("rejectReason", log.getRejectReason());
        map.put("reviewTags", log.getReviewTags());
        map.put("aiScore", log.getAiScore());
        map.put("ip", log.getIp());
        map.put("createTime", log.getCreateTime());
        return map;
    }
}
