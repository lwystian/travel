package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SysLog;
import org.example.springboot.entity.LoginLog;
import org.example.springboot.entity.ReviewLog;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/logs")
public class LogController {
    @Autowired private SysLogService sysLogService;
    @Autowired private LoginLogService loginLogService;
    @Autowired private ReviewLogService reviewLogService;
    @Autowired private BackupLogService backupLogService;

    @GetMapping("/sys")
    public Result<?> getSysLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        requireAdmin();
        List<SysLog> list = sysLogService.queryLogs(page, limit, username, startTime, endTime);
        long total = sysLogService.countLogs(username, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/login")
    public Result<?> getLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        requireAdmin();
        List<LoginLog> list = loginLogService.queryLogs(page, limit, username, startTime, endTime);
        long total = loginLogService.countLogs(username, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/review")
    public Result<?> getReviewLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        requireAdmin();
        List<ReviewLog> list = reviewLogService.queryLogs(page, limit, username, startTime, endTime);
        long total = reviewLogService.countLogs(username, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        return Result.success(result);
    }

    @PostMapping("/backup")
    public Result<?> backupLogs(@RequestBody Map<String, Object> params) {
        requireAdmin();
        String tableName = (String) params.get("tableName");
        String startTimeStr = (String) params.get("startTime");
        String endTimeStr = (String) params.get("endTime");

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));
        }
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            endTime = LocalDateTime.parse(endTimeStr.replace(" ", "T"));
        }

        Map<String, Object> result = backupLogService.backupAndExport(tableName, startTime, endTime);
        Boolean success = (Boolean) result.get("success");
        return success ? Result.success(result) : Result.error((String) result.get("message"));
    }

    @PostMapping("/backupAll")
    public Result<?> backupAllLogs() {
        requireAdmin();
        Map<String, Object> result = new HashMap<>();
        try {
            // 导出所有日志表
            backupLogService.backupAndExport("sys_log", null, null);
            backupLogService.backupAndExport("login_log", null, null);
            backupLogService.backupAndExport("review_log", null, null);
            result.put("success", true);
            result.put("message", "全部日志导出成功");
            return Result.success(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导出失败: " + e.getMessage());
            return Result.error((String) result.get("message"));
        }
    }

    @GetMapping("/backup/list")
    public Result<?> getBackupList(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int limit) {
        requireAdmin();
        Page<org.example.springboot.entity.BackupLog> pageObj = new Page<>(page, limit);
        Page<org.example.springboot.entity.BackupLog> result = backupLogService.page(pageObj);
        return Result.success(result);
    }

    private void requireAdmin() {
        SecurityGuards.requirePermission("log:view");
    }
}
