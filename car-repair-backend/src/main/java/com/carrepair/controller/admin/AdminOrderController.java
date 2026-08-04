package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizBooking;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.service.BizBookingService;
import com.carrepair.service.BizRepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端-工单管理控制器
 */
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Autowired
    private BizRepairOrderService orderService;
    
    @Autowired
    private BizBookingService bookingService;

    /**
     * 获取工单列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // 使用自定义查询，返回包含车主和维修师信息
        IPage<Map<String, Object>> pageResult = orderService.pageListWithUserInfo(orderNo, status, technicianId, page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        
        return Result.success(result);
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> detail = orderService.getDetail(id);
        if (detail == null) {
            return Result.error("工单不存在");
        }
        return Result.success(detail);
    }

    /**
     * 创建工单（客户到店）
     */
    @PostMapping("/create")
    public Result<BizRepairOrder> create(@RequestBody BizRepairOrder order) {
        // 如果关联预约，更新预约状态
        if (order.getBookingId() != null) {
            BizBooking booking = bookingService.getById(order.getBookingId());
            if (booking != null) {
                booking.setStatus(3); // 已到店
                bookingService.updateById(booking);
            }
        }
        
        BizRepairOrder result = orderService.createOrder(order);
        return Result.success("工单创建成功", result);
    }
    
    /**
     * 质检通过
     */
    @PostMapping("/qualityCheckPass/{id}")
    public Result<Void> qualityCheckPass(@PathVariable Long id) {
        boolean success = orderService.qualityCheckPass(id);
        if (success) {
            return Result.success("质检通过，工单已进入待结算状态");
        }
        return Result.error("质检失败，请检查工单状态");
    }
}
