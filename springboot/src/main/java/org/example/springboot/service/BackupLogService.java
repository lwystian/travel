package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.BackupLog;
import java.util.Map;
import java.time.LocalDateTime;

public interface BackupLogService extends IService<BackupLog> {
    Map<String, Object> backupAndExport(String tableName, LocalDateTime startTime, LocalDateTime endTime);
    Map<String, Object> queryLogs(String tableName, int page, int limit, String username, String startTime, String endTime);
}
