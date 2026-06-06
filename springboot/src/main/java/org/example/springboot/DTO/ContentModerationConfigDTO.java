package org.example.springboot.dto;

import lombok.Data;

@Data
public class ContentModerationConfigDTO {
    private Boolean adminContentReviewRequired;
    private Boolean adminGuideReviewRequired;
    private Boolean adminCommentReviewRequired;
}
