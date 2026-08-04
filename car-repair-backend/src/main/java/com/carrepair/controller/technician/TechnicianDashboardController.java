package com.carrepair.controller.technician;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrepair.common.Result;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.mapper.BizEvaluationMapper;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修端-工作台控制器
 */
@RestController
@RequestMapping("/api/technician/dashboard")
public class TechnicianDashboardController {

    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private BizEvaluationMapper evaluationMapper;

    /**
     * 获取工作台统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@AuthenticationPrincipal LoginUser loginUser) {
        Long technicianId = loginUser.getUser().getId();
        Map<String, Object> result = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        
        // 今日工单数
        long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getTechnicianId, technicianId)
                        .between(BizRepairOrder::getCreateTime, todayStart, todayEnd));
        
        // 维修中的工单数
        long inProgressOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getTechnicianId, technicianId)
                        .eq(BizRepairOrder::getStatus, 2));
        
        // 本月完成工单数
        LocalDate monthStart = today.withDayOfMonth(1);
        List<BizRepairOrder> monthOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getTechnicianId, technicianId)
                        .eq(BizRepairOrder::getStatus, 5)
                        .ge(BizRepairOrder::getEndTime, monthStart.atStartOfDay()));
        
        // 本月收入（工时费的一部分，假设技师提成30%）
        BigDecimal monthIncome = BigDecimal.ZERO;
        BigDecimal totalHours = BigDecimal.ZERO;
        for (BizRepairOrder order : monthOrders) {
            if (order.getLaborCost() != null) {
                monthIncome = monthIncome.add(order.getLaborCost().multiply(BigDecimal.valueOf(0.3)));
            }
            if (order.getActualHours() != null) {
                totalHours = totalHours.add(order.getActualHours());
            }
        }
        
        // 平均工时
        BigDecimal avgHours = monthOrders.isEmpty() ? BigDecimal.ZERO 
                : totalHours.divide(BigDecimal.valueOf(monthOrders.size()), 1, RoundingMode.HALF_UP);
        
        // 平均评分
        double avgScore = 0;
        if (!monthOrders.isEmpty()) {
            List<BizEvaluation> evaluations = evaluationMapper.selectList(
                    new LambdaQueryWrapper<BizEvaluation>()
                            .in(BizEvaluation::getOrderId, 
                                monthOrders.stream().map(BizRepairOrder::getId).toArray()));
            
            avgScore = evaluations.isEmpty() ? 0 
                    : evaluations.stream().mapToInt(BizEvaluation::getScore).average().orElse(0);
        }
        
        result.put("todayOrders", todayOrders);
        result.put("inProgressOrders", inProgressOrders);
        result.put("completedOrders", monthOrders.size());
        result.put("monthIncome", monthIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("avgHours", avgHours);
        result.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        
        return Result.success(result);
    }
}
