package com.carrepair.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizEvaluationService;
import com.carrepair.service.BizRepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 前台-工单控制器
 */
@RestController
@RequestMapping("/api/front/order")
public class FrontOrderController {

    @Autowired
    private BizRepairOrderService orderService;
    
    @Autowired
    private BizEvaluationService evaluationService;

    /**
     * 获取工单列表
     */
    @GetMapping("/list")
    public Result<PageResult<BizRepairOrder>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal LoginUser loginUser) {
        IPage<BizRepairOrder> pageResult = orderService.pageByUserId(
                loginUser.getUser().getId(), status, page, size);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, 
                                               @AuthenticationPrincipal LoginUser loginUser) {
        Map<String, Object> detail = orderService.getDetail(id);
        if (detail == null) {
            return Result.error("工单不存在");
        }
        BizRepairOrder order = (BizRepairOrder) detail.get("order");
        if (!order.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("无权查看此工单");
        }
        // 添加评价信息
        BizEvaluation evaluation = evaluationService.getByOrderId(id);
        detail.put("evaluation", evaluation);
        return Result.success(detail);
    }

    /**
     * 模拟支付
     */
    @PostMapping("/pay/{orderId}")
    public Result<Void> pay(@PathVariable Long orderId, @AuthenticationPrincipal LoginUser loginUser) {
        BizRepairOrder order = orderService.getById(orderId);
        if (order == null || !order.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("工单不存在或无权操作");
        }
        if (orderService.pay(orderId)) {
            return Result.success("支付成功");
        }
        return Result.error("支付失败");
    }
}
