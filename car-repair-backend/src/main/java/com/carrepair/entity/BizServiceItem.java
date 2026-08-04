package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项目实体类
 */
@Data
@TableName("biz_service_item")
public class BizServiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务名称 */
    private String serviceName;

    /** 分类:保养/维修/钣喷/电子 */
    private String category;

    /** 是否需要质检:0不需要 1需要 */
    private Integer needQualityCheck;

    /** 标准工时(小时) */
    private BigDecimal standardHours;

    /** 基础价格 */
    private BigDecimal basePrice;

    /** 难度系数 */
    private BigDecimal difficultyFactor;

    /** 服务描述 */
    private String description;

    /** 图片URL */
    private String imageUrl;

    /** 状态:0禁用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
