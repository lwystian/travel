package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TourHotel;
import org.example.springboot.security.SecurityGuards;
import org.example.springboot.service.TourHotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tour-hotel")
@Tag(name = "行程酒店管理接口")
public class TourHotelController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TourHotelController.class);

    @Resource
    private TourHotelService tourHotelService;

    @Operation(summary = "获取行程关联的酒店列表")
    @GetMapping("/{tourId}")
    public Result<?> getTourHotels(@PathVariable Long tourId) {
        LOGGER.info("获取行程关联的酒店列表，tourId={}", tourId);
        try {
            List<TourHotel> hotels = tourHotelService.getTourHotels(tourId);
            return Result.success(hotels);
        } catch (Exception e) {
            LOGGER.error("获取行程酒店失败", e);
            return Result.error("获取行程酒店失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取酒店详情")
    @GetMapping("/detail/{id}")
    public Result<?> getTourHotelDetail(@PathVariable Long id) {
        LOGGER.info("获取酒店详情，id={}", id);
        try {
            TourHotel hotel = tourHotelService.getTourHotelById(id);
            return Result.success(hotel);
        } catch (Exception e) {
            LOGGER.error("获取酒店详情失败", e);
            return Result.error("获取酒店详情失败：" + e.getMessage());
        }
    }

    @Operation(summary = "添加行程酒店关联")
    @PostMapping
    public Result<?> addTourHotel(@RequestBody TourHotel tourHotel) {
        SecurityGuards.requireAdmin();
        LOGGER.info("添加行程酒店关联：{}", tourHotel);
        try {
            TourHotel saved = tourHotelService.addTourHotel(tourHotel);
            return Result.success(saved);
        } catch (Exception e) {
            LOGGER.error("添加酒店关联失败", e);
            return Result.error("添加酒店关联失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新行程酒店关联")
    @PutMapping("/{id}")
    public Result<?> updateTourHotel(@PathVariable Long id, @RequestBody TourHotel tourHotel) {
        SecurityGuards.requireAdmin();
        LOGGER.info("更新行程酒店关联，id={}，数据：{}", id, tourHotel);
        try {
            tourHotel.setId(id);
            TourHotel updated = tourHotelService.updateTourHotel(tourHotel);
            return Result.success(updated);
        } catch (Exception e) {
            LOGGER.error("更新酒店关联失败", e);
            return Result.error("更新酒店关联失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除行程酒店关联")
    @DeleteMapping("/{id}")
    public Result<?> deleteTourHotel(@PathVariable Long id) {
        SecurityGuards.requireAdmin();
        LOGGER.info("删除行程酒店关联，id={}", id);
        try {
            boolean result = tourHotelService.deleteTourHotel(id);
            if (result) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除酒店关联失败");
            }
        } catch (Exception e) {
            LOGGER.error("删除酒店关联失败", e);
            return Result.error("删除酒店关联失败：" + e.getMessage());
        }
    }
}
