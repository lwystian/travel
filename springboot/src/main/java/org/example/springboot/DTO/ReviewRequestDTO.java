package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审核请求DTO
 */
@Data
@Schema(description = "审核请求参数")
public class ReviewRequestDTO {
    
    @Schema(description = "审核状态: 1-通过, 2-拒绝")
    private Integer status;
    
    @Schema(description = "审核意见")
    private String reviewComment;
}
