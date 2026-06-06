package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.annotation.OperationLog;
import org.example.springboot.common.Result;
import org.example.springboot.entity.SensitiveWord;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.SensitiveWordService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "敏感词管理")
@RestController
@RequestMapping("/sensitive-word")
public class SensitiveWordController {
    @Resource
    private SensitiveWordService sensitiveWordService;

    @Operation(summary = "分页查询敏感词")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "") String keyword,
                          @RequestParam(defaultValue = "") String category,
                          @RequestParam(defaultValue = "") String level,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        requireReviewPermission();
        Page<SensitiveWord> page = sensitiveWordService.getWordsByPage(keyword, category, level, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "新增敏感词")
    @PostMapping
    @OperationLog(operationType = "CREATE", description = "新增敏感词规则", targetType = "敏感词规则")
    public Result<?> add(@RequestBody SensitiveWord word) {
        requireReviewPermission();
        sensitiveWordService.save(word);
        return Result.success("新增成功");
    }

    @Operation(summary = "批量导入敏感词")
    @PostMapping("/batch")
    @OperationLog(operationType = "CREATE", description = "批量导入敏感词规则", targetType = "敏感词规则")
    public Result<?> batchAdd(@RequestBody List<SensitiveWord> words) {
        requireReviewPermission();
        int saved = 0;
        if (words != null) {
            for (SensitiveWord word : words) {
                if (word.getWord() == null || word.getWord().isBlank()) {
                    continue;
                }
                boolean exists = sensitiveWordService.lambdaQuery()
                        .eq(SensitiveWord::getWord, word.getWord().trim())
                        .exists();
                if (exists) {
                    continue;
                }
                word.setWord(word.getWord().trim());
                sensitiveWordService.save(word);
                saved++;
            }
        }
        return Result.success("已导入 " + saved + " 条敏感词");
    }

    @Operation(summary = "更新敏感词")
    @PutMapping("/{id}")
    @OperationLog(operationType = "UPDATE", description = "更新敏感词规则", targetType = "敏感词规则")
    public Result<?> update(@PathVariable Long id, @RequestBody SensitiveWord word) {
        requireReviewPermission();
        word.setId(id);
        sensitiveWordService.updateById(word);
        return Result.success("更新成功");
    }

    @Operation(summary = "启用或停用敏感词")
    @PutMapping("/{id}/status")
    @OperationLog(operationType = "UPDATE", description = "调整敏感词状态", targetType = "敏感词规则")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        requireReviewPermission();
        SensitiveWord word = new SensitiveWord();
        word.setId(id);
        word.setStatus(body.getOrDefault("status", 1));
        sensitiveWordService.updateById(word);
        return Result.success("状态已更新");
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    @OperationLog(operationType = "DELETE", description = "删除敏感词规则", targetType = "敏感词规则")
    public Result<?> delete(@PathVariable Long id) {
        requireReviewPermission();
        sensitiveWordService.removeById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量删除敏感词")
    @DeleteMapping("/batch")
    @OperationLog(operationType = "DELETE", description = "批量删除敏感词规则", targetType = "敏感词规则")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        requireReviewPermission();
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择需要删除的敏感词");
        }
        sensitiveWordService.removeBatchByIds(ids);
        sensitiveWordService.refreshCache();
        return Result.success("批量删除成功");
    }

    @Operation(summary = "测试敏感词过滤")
    @PostMapping("/check")
    @OperationLog(operationType = "CHECK", description = "测试敏感词规则", targetType = "敏感词规则", logParams = true)
    public Result<?> check(@RequestBody Map<String, String> body) {
        requireReviewPermission();
        String content = body.getOrDefault("content", "");
        return Result.success(sensitiveWordService.checkText(content, "测试内容", null));
    }

    private void requireReviewPermission() {
        SecurityGuards.requirePermission("review:manage");
    }
}
