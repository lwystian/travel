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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-msvideo",
            "video/avi"
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
        String baseDir = Objects.requireNonNull(fileType.getTypeName(), "file type name must not be null");
        String path = FileUtil.saveFile(file, null, baseDir);
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(path)) {
            return Result.success(withVersion(path));
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
        List<String> returnedPaths = new ArrayList<>();
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
                String baseDir = Objects.requireNonNull(FileType.COMMON.getTypeName(), "file type name must not be null");
                String path = FileUtil.saveFile(file, null, baseDir);
                if (StringUtils.hasText(path)) {
                    successPaths.add(path);
                    returnedPaths.add(withVersion(path));
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
                File uploadedFile = toStoredFile(path);
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
        return returnedPaths;
    }

    private File toStoredFile(String returnedPath) {
        String relativePath = returnedPath == null ? "" : returnedPath.split("\\?", 2)[0];
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path basePath = Paths.get(FileUtil.FILE_BASE_PATH).normalize();
        Path storedPath = basePath.resolve(relativePath).normalize();
        if (!storedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return storedPath.toFile();
    }

    private String withVersion(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + "v=" + System.currentTimeMillis();
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
        if (FileType.VIDEO.equals(fileType)) {
            String contentType = file.getContentType();
            if (contentType == null || !VIDEO_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                LOGGER.warn("Rejected video upload by content type: filename={}, contentType={}", file.getOriginalFilename(), contentType);
                return false;
            }
        }
        return true;
    }
}
