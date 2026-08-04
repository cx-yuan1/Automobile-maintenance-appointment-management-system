package com.carrepair.controller.front;

import com.carrepair.common.Result;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizEvaluationService;
import com.carrepair.service.BizRepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 前台-评价控制器
 */
@RestController
@RequestMapping("/api/front/evaluation")
public class FrontEvaluationController {

    @Autowired
    private BizEvaluationService evaluationService;
    
    @Autowired
    private BizRepairOrderService orderService;

    /**
     * 提交评价
     */
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody BizEvaluation evaluation, 
                               @AuthenticationPrincipal LoginUser loginUser) {
        // 验证工单归属
        BizRepairOrder order = orderService.getById(evaluation.getOrderId());
        if (order == null || !order.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("工单不存在或无权操作");
        }
        
        // 检查是否已评价
        if (evaluationService.getByOrderId(evaluation.getOrderId()) != null) {
            return Result.error("该工单已评价");
        }
        
        evaluation.setUserId(loginUser.getUser().getId());
        if (evaluationService.save(evaluation)) {
            return Result.success("评价成功");
        }
        return Result.error("评价失败");
    }
}
