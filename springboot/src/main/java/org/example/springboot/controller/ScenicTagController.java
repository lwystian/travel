package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ScenicTag;
import org.example.springboot.service.ScenicTagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "景点标签管理接口")
@RestController
@RequestMapping("/scenic-tag")
public class ScenicTagController {
    
    @Resource
    private ScenicTagService scenicTagService;
    
    @Operation(summary = "获取所有标签")
    @GetMapping("/all")
    public Result<?> getAllTags() {
        List<ScenicTag> tags = scenicTagService.getAllTags();
        return Result.success(tags);
    }
    
    @Operation(summary = "获取景点关联的标签")
    @GetMapping("/scenic/{scenicSpotId}")
    public Result<?> getTagsByScenicSpotId(@PathVariable Long scenicSpotId) {
        List<ScenicTag> tags = scenicTagService.getTagsByScenicSpotId(scenicSpotId);
        return Result.success(tags);
    }
    
    @Operation(summary = "新增标签")
    @PostMapping("/add")
    public Result<?> createTag(@RequestBody ScenicTag tag) {
        scenicTagService.save(tag);
        return Result.success("新增成功");
    }
    
    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<?> updateTag(@PathVariable Long id, @RequestBody ScenicTag tag) {
        tag.setId(id);
        scenicTagService.updateById(tag);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<?> deleteTag(@PathVariable Long id) {
        scenicTagService.removeById(id);
        return Result.success("删除成功");
    }
}
