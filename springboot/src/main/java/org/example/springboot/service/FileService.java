package org.example.springboot.service;

import io.swagger.v3.oas.annotations.Operation;
import org.example.springboot.common.Result;
import org.example.springboot.enumClass.FileType;
import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_COMMON_SIZE = 100 * 1024 * 1024;
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/x-icon",
            "image/vnd.microsoft.icon"
    );

    @Operation(summary = "文件上传")
    public Result<?> upLoad(MultipartFile file, FileType fileType) {
        if (file == null || com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(file.getOriginalFilename())) {
            LOGGER.error("File does not exist");
            return Result.error("-1", "文件不存在");
        }
        if (!isAllowedFile(file, fileType)) {
            return Result.error("-1", "文件类型或大小不符合安全要求");
        }

        LOGGER.info("upload FILE:{}", file.getOriginalFilename());
        String path = FileUtil.saveFile(file, null, fileType.getTypeName());
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(path)) {
            return Result.success(path);
        }
        return Result.error("-1", "文件上传失败");
    }

    @DeleteMapping("/remove/{filename}")
    public Result<?> fileRemove(@PathVariable String filename) {
        String filePath = "img/" + filename;
        boolean res = FileUtil.deleteFile(filePath);
        return res ? Result.success() : Result.error("-1", "删除失败");
    }

    public List<String> uploadMultiple(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            LOGGER.error("No files uploaded");
            return null;
        }

        List<String> successPaths = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (file == null || !StringUtils.hasText(file.getOriginalFilename())) {
                    failedFiles.add("empty file");
                    continue;
                }
                if (!isAllowedFile(file, FileType.COMMON)) {
                    failedFiles.add(file.getOriginalFilename() + ": 文件类型或大小不符合安全要求");
                    continue;
                }
                LOGGER.info("upload FILE:{}", file.getOriginalFilename());
                String path = FileUtil.saveFile(file, null, FileType.COMMON.getTypeName());
                if (StringUtils.hasText(path)) {
                    successPaths.add(path);
                } else {
                    failedFiles.add(file.getOriginalFilename() + ": 文件上传失败");
                }
            } catch (Exception e) {
                LOGGER.error("File upload failed: {}", file != null ? file.getOriginalFilename() : "null", e);
                failedFiles.add(file != null ? file.getOriginalFilename() : "null");
            }
        }

        if (!failedFiles.isEmpty()) {
            for (String path : successPaths) {
                File uploadedFile = new File(path);
                if (uploadedFile.exists() && uploadedFile.isFile()) {
                    if (uploadedFile.delete()) {
                        LOGGER.info("Deleted successfully uploaded file: {}", path);
                    } else {
                        LOGGER.warn("Failed to delete file: {}", path);
                    }
                }
            }
            return null;
        }
        return successPaths;
    }

    private boolean isAllowedFile(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        long maxSize = FileType.IMG.equals(fileType) ? MAX_IMAGE_SIZE : MAX_COMMON_SIZE;
        if (file.getSize() > maxSize) {
            LOGGER.warn("Rejected upload by size: filename={}, size={}", file.getOriginalFilename(), file.getSize());
            return false;
        }
        if (FileType.IMG.equals(fileType)) {
            String contentType = file.getContentType();
            if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                LOGGER.warn("Rejected image upload by content type: filename={}, contentType={}", file.getOriginalFilename(), contentType);
                return false;
            }
        }
        return true;
    }
}
