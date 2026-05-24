package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新密码DTO")
public class UserPasswordUpdateDTO {
    @Schema(description = "旧密码")
    private String oldPassword;
    @Schema(description = "新密码")
    private String newPassword;
    @Schema(description = "当前绑定手机号验证码，修改密码必填")
    private String currentPhoneCode;
}
