package org.example.springboot.task;

import jakarta.annotation.Resource;
import org.example.springboot.service.SysOperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SysOperationLogCleanupTask {
    private static final Logger logger = LoggerFactory.getLogger(SysOperationLogCleanupTask.class);

    @Resource
    private SysOperationLogService sysOperationLogService;

    @Value("${system.log.retention-months:6}")
    private int retentionMonths;

    @Scheduled(cron = "${system.log.cleanup-cron:0 30 2 * * ?}")
    public void cleanExpiredLogs() {
        int safeRetentionMonths = Math.max(retentionMonths, 1);
        LocalDateTime beforeTime = LocalDateTime.now().minusMonths(safeRetentionMonths);
        int count = sysOperationLogService.deleteBefore(beforeTime);
        logger.info("Expired system logs cleaned: retentionMonths={}, beforeTime={}, deleted={}",
                safeRetentionMonths, beforeTime, count);
    }
}
