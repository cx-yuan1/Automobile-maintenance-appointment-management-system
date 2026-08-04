package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配件实体类
 */
@Data
@TableName("biz_part")
public class BizPart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配件编码 */
    private String partCode;

    /** 配件名称 */
    private String partName;

    /** 分类 */
    private String category;

    /** 品牌 */
    private String brand;

    /** 单位 */
    private String unit;

    /** 采购价 */
    private BigDecimal purchasePrice;

    /** 销售价 */
    private BigDecimal salePrice;

    /** 库存数量 */
    private Integer stockQuantity;

    /** 预留数量 */
    private Integer reservedQuantity;

    /** 最低库存(预警阈值) */
    private Integer minStock;

    /** 图片URL */
    private String imageUrl;

    /** 状态:0停用 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
