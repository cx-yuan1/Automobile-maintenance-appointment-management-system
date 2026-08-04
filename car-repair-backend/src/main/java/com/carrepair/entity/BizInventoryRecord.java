package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存流水实体类
 */
@Data
@TableName("biz_inventory_record")
public class BizInventoryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配件ID */
    private Long partId;

    /** 类型:1入库 2出库 3预留 4释放预留 */
    private Integer recordType;

    /** 数量 */
    private Integer quantity;

    /** 变更前数量 */
    private Integer beforeQuantity;

    /** 变更后数量 */
    private Integer afterQuantity;

    /** 关联工单ID */
    private Long relatedOrderId;

    /** 操作人ID */
    private Long operatorId;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
