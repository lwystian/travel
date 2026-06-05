package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.enumClass.FileType;
import org.example.springboot.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "文件上传接口类")
@RequestMapping("/file")
@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @Operation(summary = "文件上传")
    @PostMapping("/upload/img")
    public Result<?> upLoad(@RequestParam("file") MultipartFile file) {
      return   fileService.upLoad(file, FileType.IMG);
    }

    @Operation(summary = "获取图片上传配置")
    @GetMapping("/upload/img/config")
    public Result<?> getImageUploadConfig() {
        return Result.success(Map.of(
                "maxSizeBytes", fileService.getMaxImageSizeBytes()
        ));
    }

    @Operation(summary = "video upload")
    @PostMapping("/upload/video")
    public Result<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        return fileService.upLoad(file, FileType.VIDEO);
    }
    @Operation(summary = "多文件上传，并且在有失败时删除已上传成功的文件")
    @PostMapping("/uploadMultiple")
    public Result<?> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<String> strings = fileService.uploadMultiple(files);
        return strings != null && !strings.isEmpty() ? Result.success(strings) : Result.error("-1", "文件上传失败！");
    }
}
