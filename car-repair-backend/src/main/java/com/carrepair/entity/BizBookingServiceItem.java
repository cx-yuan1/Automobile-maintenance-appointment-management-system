package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 预约服务关联实体类
 */
@Data
@TableName("biz_booking_service")
public class BizBookingServiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long bookingId;

    /** 服务项目ID */
    private Long serviceId;

    /** 数量 */
    private Integer quantity;
}
