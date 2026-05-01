package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 常用出行人信息实体类
 */
@Data
@TableName("frequent_traveler")
@Schema(description = "常用出行人信息实体类")
public class FrequentTraveler {

    @TableId(type = IdType.AUTO)
    @Schema(description = "出行人ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "证件类型: ID_CARD-身份证, PASSPORT-护照")
    private String idType;

    @Schema(description = "证件号码")
    private String idNumber;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "性别: MALE-男, FEMALE-女")
    private String gender;

    @Schema(description = "类型: ADULT-成人, CHILD-儿童")
    private String travelerType;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "是否默认")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
