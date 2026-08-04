package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务评价实体类
 */
@Data
@TableName("biz_evaluation")
public class BizEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long orderId;

    /** 客户ID */
    private Long userId;

    /** 评分(1-5) */
    private Integer score;

    /** 评价内容 */
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
