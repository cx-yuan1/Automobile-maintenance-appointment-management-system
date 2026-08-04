package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定价配置实体类
 */
@Data
@TableName("biz_pricing_config")
public class BizPricingConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置类型:VEHICLE_TYPE/SEASON/INVENTORY/VIP_LEVEL */
    private String configType;

    /** 配置键 */
    private String configKey;

    /** 配置名称 */
    private String configName;

    /** 系数值 */
    private BigDecimal factorValue;

    /** 描述 */
    private String description;

    /** 状态:0禁用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
