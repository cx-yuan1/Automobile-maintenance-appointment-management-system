package com.carrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizEvaluation;

public interface BizEvaluationService extends IService<BizEvaluation> {
    
    /** 根据工单ID获取评价 */
    BizEvaluation getByOrderId(Long orderId);
}
