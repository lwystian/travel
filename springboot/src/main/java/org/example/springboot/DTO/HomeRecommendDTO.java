package org.example.springboot.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HomeRecommendDTO {
    
    private Long id;
    private String type;
    private Long tourId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    
    // 行程详情
    private Long tourIdDetail;
    private String tourCode;
    private String title;
    private String subtitle;
    private String mainImage;
    private String tourType;
    private Integer days;
    private BigDecimal minPrice;
    private Integer status;
}
