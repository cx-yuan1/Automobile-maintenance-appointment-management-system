package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 维修进度实体类
 */
@Data
@TableName("biz_repair_progress")
public class BizRepairProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 进度状态 */
    private String progressStatus;

    /** 进度描述 */
    private String progressDesc;

    /** 图片URL(逗号分隔) */
    private String images;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
