package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_provider_config")
public class AuthProviderConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String configType;

    private String configName;

    private Boolean enabled;

    private String configData;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
