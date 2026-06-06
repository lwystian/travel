package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SysOperationLog;
import org.example.springboot.security.RolePermission;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.SysOperationLogService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "系统日志管理")
@RestController
@RequestMapping("/sys/log")
public class SysLogController {
    private static final String CSV_LINE_SEPARATOR = "\r\n";

    @Resource
    private SysOperationLogService sysOperationLogService;

    @Operation(summary = "分页查询系统日志")
    @GetMapping("/page")
    public Result<?> getLogsByPage(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String logLevel,
            @RequestParam(defaultValue = "") String startTime,
            @RequestParam(defaultValue = "") String endTime,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer size) {

        SecurityGuards.requirePermission("log:view");

        Page<SysOperationLog> page = sysOperationLogService.getLogsByPage(
                username, roleCode, operationType, logLevel, startTime, endTime, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "导出系统日志")
    @GetMapping("/export")
    @OperationLog(operationType = "EXPORT", description = "备份系统日志", targetType = "系统日志", logParams = false)
    public void exportLogs(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String logLevel,
            @RequestParam(defaultValue = "") String startTime,
            @RequestParam(defaultValue = "") String endTime,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "true") boolean includeUserAgent,
            @RequestParam(defaultValue = "true") boolean includeParams,
            HttpServletResponse response) throws IOException {

        if (!hasLogPermission()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"403\",\"msg\":\"权限不足，请联系管理员\"}");
            return;
        }

        List<SysOperationLog> logs = sysOperationLogService.getAllLogs(
                username, roleCode, operationType, logLevel, startTime, endTime, ids);
        String filename = "系统日志备份_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));

        StringBuilder content = new StringBuilder();
        content.append('\ufeff')
                .append(buildCsvHeader(includeUserAgent, includeParams))
                .append(CSV_LINE_SEPARATOR);
        for (SysOperationLog log : logs) {
            content.append(toCsvLine(log, includeUserAgent, includeParams));
        }
        response.getOutputStream().write(content.toString().getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    @Operation(summary = "根据ID删除系统日志")
    @DeleteMapping("/{id}")
    @OperationLog(operationType = "DELETE", description = "删除系统日志", targetType = "系统日志")
    public Result<?> deleteById(@PathVariable Long id) {
        SecurityGuards.requirePermission("log:view");

        sysOperationLogService.deleteById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量删除系统日志")
    @DeleteMapping("/batch")
    @OperationLog(operationType = "DELETE", description = "批量删除系统日志", targetType = "系统日志")
    public Result<?> deleteBatch(@RequestParam List<Long> ids) {
        SecurityGuards.requirePermission("log:view");

        sysOperationLogService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "清理指定天数之前的系统日志")
    @DeleteMapping("/clean/{days}")
    @OperationLog(operationType = "DELETE", description = "清理过期系统日志", targetType = "系统日志")
    public Result<?> cleanOldLogs(@PathVariable Integer days) {
        SecurityGuards.requirePermission("log:view");

        int safeDays = Math.max(days == null ? 180 : days, 1);
        int count = sysOperationLogService.deleteBefore(LocalDateTime.now().minusDays(safeDays));
        return Result.success("已清理 " + count + " 条过期日志");
    }

    private String buildCsvHeader(boolean includeUserAgent, boolean includeParams) {
        StringBuilder header = new StringBuilder("日志ID,时间,等级,操作人,身份,操作类型,操作说明,操作对象,请求方法,请求地址,IP,端口,结果,耗时(ms),错误信息,设备ID,设备指纹,客户端硬件特征,MAC地址");
        if (includeUserAgent) {
            header.append(",User-Agent");
        }
        if (includeParams) {
            header.append(",请求参数");
        }
        return header.toString();
    }

    private String toCsvLine(SysOperationLog log, boolean includeUserAgent, boolean includeParams) {
        StringBuilder line = new StringBuilder();
        line.append(csv(log.getId()))
                .append(",").append(csv(log.getCreateTime()))
                .append(",").append(csv(normalizeLogLevel(log)))
                .append(",").append(csv(log.getUsername()))
                .append(",").append(csv(formatRole(log)))
                .append(",").append(csv(log.getOperationType()))
                .append(",").append(csv(log.getOperationDesc()))
                .append(",").append(csv(log.getTargetType()))
                .append(",").append(csv(log.getRequestMethod()))
                .append(",").append(csv(log.getRequestUrl()))
                .append(",").append(csv(log.getIpAddress()))
                .append(",").append(csv(log.getPort()))
                .append(",").append(csv(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败"))
                .append(",").append(csv(log.getExecutionTime()))
                .append(",").append(csv(log.getErrorMessage()))
                .append(",").append(csv(log.getDeviceId()))
                .append(",").append(csv(log.getDeviceFingerprint()))
                .append(",").append(csv(log.getClientHardware()))
                .append(",").append(csv(log.getMacAddress()));
        if (includeUserAgent) {
            line.append(",").append(csv(log.getUserAgent()));
        }
        if (includeParams) {
            line.append(",").append(csv(log.getRequestParams()));
        }
        return line.append(CSV_LINE_SEPARATOR).toString();
    }

    private String normalizeLogLevel(SysOperationLog log) {
        if (log.getLogLevel() != null && !log.getLogLevel().isBlank()) {
            return log.getLogLevel();
        }
        if (log.getErrorMessage() != null && !log.getErrorMessage().isBlank()) {
            return "ERROR";
        }
        if (log.getStatus() != null && log.getStatus() == 0) {
            return "WARN";
        }
        return "INFO";
    }

    private String formatRole(SysOperationLog log) {
        if (log.getRoleName() != null && !log.getRoleName().isBlank()) {
            return log.getRoleName();
        }
        if (log.getRoleCode() != null && !log.getRoleCode().isBlank()) {
            return RolePermission.roleNameOf(log.getRoleCode());
        }
        return "";
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private boolean hasLogPermission() {
        try {
            SecurityGuards.requirePermission("log:view");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
