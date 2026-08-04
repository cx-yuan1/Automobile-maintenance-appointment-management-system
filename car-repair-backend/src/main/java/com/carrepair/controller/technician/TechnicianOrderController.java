package com.carrepair.controller.technician;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.BizRepairProgress;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizRepairOrderService;
import com.carrepair.service.BizRepairProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 维修端-工单控制器
 */
@RestController
@RequestMapping("/api/technician/order")
public class TechnicianOrderController {

    @Autowired
    private BizRepairOrderService orderService;
    
    @Autowired
    private BizRepairProgressService progressService;

    /**
     * 获取待处理工单列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal LoginUser loginUser) {
        IPage<Map<String, Object>> pageResult = orderService.pageListWithUserInfo(
                null, status, loginUser.getUser().getId(), page, size);
        return Result.success(PageResult.of(pageResult));
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
     * 确认完工
     */
    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody Map<String, Object> params, 
                                 @AuthenticationPrincipal LoginUser loginUser) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        BigDecimal actualHours = new BigDecimal(params.get("actualHours").toString());
        
        BizRepairOrder order = orderService.getById(orderId);
        if (order == null || !order.getTechnicianId().equals(loginUser.getUser().getId())) {
            return Result.error("工单不存在或无权操作");
        }
        
        if (orderService.complete(orderId, actualHours)) {
            // 添加完工进度记录
            BizRepairProgress progress = new BizRepairProgress();
            progress.setOrderId(orderId);
            progress.setProgressStatus("维修完成");
            progress.setProgressDesc(params.get("remark") != null ? params.get("remark").toString() : "维修已完成");
            progress.setOperatorId(loginUser.getUser().getId());
            progress.setOperatorName(loginUser.getUser().getRealName());
            progressService.addProgress(progress);
            
            return Result.success("完工确认成功");
        }
        return Result.error("完工确认失败");
    }
}
