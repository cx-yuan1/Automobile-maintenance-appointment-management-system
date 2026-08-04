package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 工单配件实体类
 */
@Data
@TableName("biz_order_part")
public class BizOrderPart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 配件ID */
    private Long partId;

    /** 配件名称 */
    private String partName;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 状态:1待领料 2已领料 */
    private Integer status;
}
