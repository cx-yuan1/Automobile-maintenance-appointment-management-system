package com.carrepair.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizBooking;
import com.carrepair.entity.BizEvaluation;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.SysUser;
import com.carrepair.mapper.BizEvaluationMapper;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.mapper.SysUserMapper;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizBookingService;
import com.carrepair.service.PricingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 前台-预约管理控制器
 */
@RestController
@RequestMapping("/api/front/booking")
public class FrontBookingController {

    @Autowired
    private BizBookingService bookingService;
    
    @Autowired
    private PricingEngine pricingEngine;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private BizEvaluationMapper evaluationMapper;

    /**
     * 获取维修师列表（带评分和完成工单数）
     */
    @GetMapping("/technicians")
    public Result<List<Map<String, Object>>> technicians() {
        // 获取所有技师
        List<SysUser> technicians = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, 2)
                        .eq(SysUser::getStatus, 1));
        
        // 获取已完成的工单
        List<BizRepairOrder> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>().eq(BizRepairOrder::getStatus, 5));
        
        // 按技师统计工单数
        Map<Long, Integer> technicianOrderCount = new HashMap<>();
        for (BizRepairOrder order : completedOrders) {
            if (order.getTechnicianId() != null) {
                technicianOrderCount.merge(order.getTechnicianId(), 1, Integer::sum);
            }
        }
        
        // 获取技师评分
        List<BizEvaluation> evaluations = evaluationMapper.selectList(null);
        Map<Long, List<Integer>> technicianScores = new HashMap<>();
        for (BizEvaluation eval : evaluations) {
            // 通过工单找到技师
            BizRepairOrder order = orderMapper.selectById(eval.getOrderId());
            if (order != null && order.getTechnicianId() != null) {
                technicianScores.computeIfAbsent(order.getTechnicianId(), k -> new ArrayList<>()).add(eval.getScore());
            }
        }
        
        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser tech : technicians) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tech.getId());
            item.put("username", tech.getUsername());
            item.put("realName", tech.getRealName());
            item.put("completedOrders", technicianOrderCount.getOrDefault(tech.getId(), 0));
            
            List<Integer> scores = technicianScores.get(tech.getId());
            if (scores != null && !scores.isEmpty()) {
                double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                item.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
            } else {
                item.put("avgScore", null);
            }
            result.add(item);
        }
        
        // 按完成工单数降序排序
        result.sort((a, b) -> ((Integer) b.get("completedOrders")).compareTo((Integer) a.get("completedOrders")));
        
        return Result.success(result);
    }

    /**
     * 获取可预约时间槽
     */
    @GetMapping("/timeSlots")
    public Result<List<Map<String, Object>>> timeSlots(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(bookingService.getTimeSlots(date));
    }

    /**
     * 获取智能报价
     */
    @PostMapping("/quotation")
    public Result<Map<String, Object>> quotation(@RequestBody Map<String, Object> params) {
        Long vehicleId = Long.valueOf(params.get("vehicleId").toString());
        @SuppressWarnings("unchecked")
        List<Long> serviceIds = ((List<Integer>) params.get("serviceIds"))
                .stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        LocalDate bookingDate = LocalDate.parse(params.get("bookingDate").toString());
        
        Map<String, Object> result = pricingEngine.calculateQuotation(vehicleId, serviceIds, bookingDate);
        return Result.success(result);
    }

    /**
     * 创建预约
     */
    @PostMapping("/create")
    public Result<BizBooking> create(@RequestBody Map<String, Object> params, 
                                     @AuthenticationPrincipal LoginUser loginUser) {
        BizBooking booking = new BizBooking();
        booking.setUserId(loginUser.getUser().getId());
        booking.setVehicleId(Long.valueOf(params.get("vehicleId").toString()));
        booking.setBookingDate(LocalDate.parse(params.get("bookingDate").toString()));
        booking.setTimeSlot(params.get("timeSlot").toString());
        booking.setRemark(params.get("remark") != null ? params.get("remark").toString() : null);
        
        // 支持指定维修师
        if (params.get("technicianId") != null) {
            booking.setTechnicianId(Long.valueOf(params.get("technicianId").toString()));
        }
        
        if (params.get("estimatedPrice") != null) {
            booking.setEstimatedPrice(new java.math.BigDecimal(params.get("estimatedPrice").toString()));
        }
        
        @SuppressWarnings("unchecked")
        List<Long> serviceIds = params.get("serviceIds") != null 
                ? ((List<Integer>) params.get("serviceIds")).stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList())
                : null;
        
        BizBooking result = bookingService.createBooking(booking, serviceIds);
        return Result.success("预约成功", result);
    }

    /**
     * 获取预约列表
     */
    @GetMapping("/list")
    public Result<PageResult<BizBooking>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal LoginUser loginUser) {
        IPage<BizBooking> pageResult = bookingService.pageByUserId(
                loginUser.getUser().getId(), status, page, size);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 取消预约
     */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        BizBooking booking = bookingService.getById(id);
        if (booking == null || !booking.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("预约不存在或无权操作");
        }
        if (bookingService.cancel(id)) {
            return Result.success("取消成功");
        }
        return Result.error("取消失败");
    }
}
