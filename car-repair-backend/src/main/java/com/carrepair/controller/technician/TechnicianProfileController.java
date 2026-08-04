package com.carrepair.controller.technician;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrepair.common.Result;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.SysUser;
import com.carrepair.mapper.BizEvaluationMapper;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.mapper.SysUserMapper;
import com.carrepair.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修端-个人中心控制器
 */
@RestController
@RequestMapping("/api/technician/profile")
public class TechnicianProfileController {

    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private BizEvaluationMapper evaluationMapper;

    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Result<SysUser> info(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getUser().getId());
        if (user != null) {
            user.setPassword(null); // 隐藏密码
        }
        return Result.success(user);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser user, 
                               @AuthenticationPrincipal LoginUser loginUser) {
        user.setId(loginUser.getUser().getId());
        user.setPassword(null); // 不允许通过此接口修改密码
        user.setUsername(null); // 不允许修改用户名
        user.setUserType(null); // 不允许修改用户类型
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> password(@RequestBody Map<String, String> params, 
                                 @AuthenticationPrincipal LoginUser loginUser) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        SysUser user = userMapper.selectById(loginUser.getUser().getId());
        if (!user.getPassword().equals(oldPassword)) {
            return Result.error("原密码错误");
        }
        
        user.setPassword(newPassword);
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    /**
     * 获取工作统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@AuthenticationPrincipal LoginUser loginUser) {
        Long technicianId = loginUser.getUser().getId();
        Map<String, Object> result = new HashMap<>();
        
        // 累计完成工单
        List<BizRepairOrder> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getTechnicianId, technicianId)
                        .eq(BizRepairOrder::getStatus, 5));
        
        // 累计收入（工时费的30%）
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (BizRepairOrder order : allOrders) {
            if (order.getLaborCost() != null) {
                totalIncome = totalIncome.add(order.getLaborCost().multiply(BigDecimal.valueOf(0.3)));
            }
        }
        
        // 平均评分
        double avgScore = 0;
        if (!allOrders.isEmpty()) {
            List<BizEvaluation> evaluations = evaluationMapper.selectList(
                    new LambdaQueryWrapper<BizEvaluation>()
                            .in(BizEvaluation::getOrderId, 
                                allOrders.stream().map(BizRepairOrder::getId).toArray()));
            
            avgScore = evaluations.isEmpty() ? 0 
                    : evaluations.stream().mapToInt(BizEvaluation::getScore).average().orElse(0);
        }
        
        // 本月统计
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<BizRepairOrder> monthOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getTechnicianId, technicianId)
                        .eq(BizRepairOrder::getStatus, 5)
                        .ge(BizRepairOrder::getEndTime, monthStart.atStartOfDay()));
        
        BigDecimal monthIncome = BigDecimal.ZERO;
        for (BizRepairOrder order : monthOrders) {
            if (order.getLaborCost() != null) {
                monthIncome = monthIncome.add(order.getLaborCost().multiply(BigDecimal.valueOf(0.3)));
            }
        }
        
        result.put("totalOrders", allOrders.size());
        result.put("totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        result.put("monthOrders", monthOrders.size());
        result.put("monthIncome", monthIncome.setScale(2, RoundingMode.HALF_UP));
        
        return Result.success(result);
    }
}
