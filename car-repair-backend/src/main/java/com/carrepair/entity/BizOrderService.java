package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 工单服务项实体类
 */
@Data
@TableName("biz_order_service")
public class BizOrderService {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 服务项目ID */
    private Long serviceId;

    /** 服务名称 */
    private String serviceName;

    /** 标准工时 */
    private BigDecimal standardHours;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 状态:1待处理 2进行中 3已完成 */
    private Integer status;
}
