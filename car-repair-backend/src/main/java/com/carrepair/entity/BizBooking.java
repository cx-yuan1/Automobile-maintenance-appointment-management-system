package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约实体类
 */
@Data
@TableName("biz_booking")
public class BizBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约编号 */
    private String bookingNo;

    /** 客户ID */
    private Long userId;

    /** 车辆ID */
    private Long vehicleId;

    /** 预约日期 */
    private LocalDate bookingDate;

    /** 预约时段 */
    private String timeSlot;

    /** 预估价格 */
    private BigDecimal estimatedPrice;
    
    /** 指定维修师ID（可选） */
    private Long technicianId;

    /** 状态:1待确认 2已确认 3已到店 4已取消 5已完成 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 维修人员名称（非数据库字段，用于前端展示） */
    @TableField(exist = false)
    private String technicianName;
    
    /** 车主名称（非数据库字段，用于前端展示） */
    @TableField(exist = false)
    private String customerName;
}
