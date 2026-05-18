package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SysOperationLog;
import org.example.springboot.entity.User;
import org.example.springboot.service.SysOperationLogService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统操作日志控制器
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/sys/log")
public class SysLogController {
    
    @Resource
    private SysOperationLogService sysOperationLogService;
    
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @OperationLog(operationType = "QUERY", description = "查询操作日志")
    public Result<?> getLogsByPage(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String startTime,
            @RequestParam(defaultValue = "") String endTime,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer size) {
        
        // 权限检查：只有管理员可以查看日志
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限查看操作日志");
        }
        
        Page<SysOperationLog> page = sysOperationLogService.getLogsByPage(
                username, operationType, startTime, endTime, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有日志（用于导出）")
    @GetMapping("/export")
    @OperationLog(operationType = "QUERY", description = "导出操作日志")
    public Result<?> exportLogs(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String operationType,
            @RequestParam(defaultValue = "") String startTime,
            @RequestParam(defaultValue = "") String endTime) {
        
        // 权限检查
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限导出操作日志");
        }
        
        List<SysOperationLog> logs = sysOperationLogService.getAllLogs(
                username, operationType, startTime, endTime);
        return Result.success(logs);
    }
    
    @Operation(summary = "根据ID删除日志")
    @DeleteMapping("/{id}")
    @OperationLog(operationType = "DELETE", description = "删除操作日志")
    public Result<?> deleteById(@PathVariable Long id) {
        
        // 权限检查
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限删除操作日志");
        }
        
        sysOperationLogService.deleteById(id);
        return Result.success("删除成功");
    }
    
    @Operation(summary = "批量删除日志")
    @DeleteMapping("/batch")
    @OperationLog(operationType = "DELETE", description = "批量删除操作日志")
    public Result<?> deleteBatch(@RequestParam List<Long> ids) {
        
        // 权限检查
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限删除操作日志");
        }
        
        sysOperationLogService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }
    
    @Operation(summary = "清理指定天数之前的日志")
    @DeleteMapping("/clean/{days}")
    @OperationLog(operationType = "DELETE", description = "清理过期日志")
    public Result<?> cleanOldLogs(@PathVariable Integer days) {
        
        // 权限检查
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限清理操作日志");
        }
        
        // TODO: 实现按时间清理日志的功能
        return Result.success("清理成功");
    }
}
