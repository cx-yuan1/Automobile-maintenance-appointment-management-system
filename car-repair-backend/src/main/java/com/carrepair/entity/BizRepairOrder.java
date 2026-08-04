package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修工单实体类
 */
@Data
@TableName("biz_repair_order")
public class BizRepairOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号 */
    private String orderNo;

    /** 关联预约ID */
    private Long bookingId;

    /** 客户ID */
    private Long userId;

    /** 车辆ID */
    private Long vehicleId;

    /** 维修人员ID */
    private Long technicianId;

    /** 状态:1待接待 2维修中 3待质检 4待结算 5已完成 6已取消 */
    private Integer status;

    /** 是否需要质检:0不需要 1需要 */
    private Integer needQualityCheck;

    /** 进店里程 */
    private Integer arrivalMileage;

    /** 故障描述 */
    private String faultDesc;

    /** 工时费 */
    private BigDecimal laborCost;

    /** 配件费 */
    private BigDecimal partsCost;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 实际工时 */
    private BigDecimal actualHours;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime endTime;

    /** 支付状态:0未支付 1已支付 */
    private Integer paymentStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
