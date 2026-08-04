package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.mapper.BizEvaluationMapper;
import com.carrepair.service.BizEvaluationService;
import org.springframework.stereotype.Service;

@Service
public class BizEvaluationServiceImpl extends ServiceImpl<BizEvaluationMapper, BizEvaluation> implements BizEvaluationService {

    @Override
    public BizEvaluation getByOrderId(Long orderId) {
        return getOne(new LambdaQueryWrapper<BizEvaluation>().eq(BizEvaluation::getOrderId, orderId));
    }
}
