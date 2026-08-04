package com.carrepair.controller.technician;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrepair.common.Result;
import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizPartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修端-配件申领控制器
 */
@RestController
@RequestMapping("/api/technician/part")
public class TechnicianPartController {

    @Autowired
    private BizPartService partService;
    
    @Autowired
    private BizPartMapper partMapper;
    
    @Autowired
    private BizOrderPartMapper orderPartMapper;
    
    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private BizInventoryRecordMapper inventoryRecordMapper;

    /**
     * 获取可用配件列表
     * 返回库存充足的配件
     */
    @GetMapping("/available")
    public Result<List<BizPart>> available() {
        List<BizPart> parts = partMapper.selectList(
                new LambdaQueryWrapper<BizPart>()
                        .eq(BizPart::getStatus, 1)
                        .apply("stock_quantity - reserved_quantity > 0")
                        .orderByAsc(BizPart::getPartName));
        return Result.success(parts);
    }

    /**
     * 获取配件列表（分页）
     * 用于配件库存查询页面
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword) {
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BizPart> pageParam = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        LambdaQueryWrapper<BizPart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizPart::getStatus, 1);
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(BizPart::getPartName, keyword)
                    .or().like(BizPart::getPartCode, keyword));
        }
        
        wrapper.orderByAsc(BizPart::getPartName);
        
        com.baomidou.mybatisplus.core.metadata.IPage<BizPart> result = partMapper.selectPage(pageParam, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        
        return Result.success(data);
    }

    /**
     * 申领配件
     * @param params 包含orderId, partId, quantity
     */
    @PostMapping("/apply")
    @Transactional
    public Result<Void> apply(@RequestBody Map<String, Object> params,
                              @AuthenticationPrincipal LoginUser loginUser) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        Long partId = Long.valueOf(params.get("partId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        
        // 验证工单
        BizRepairOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("工单不存在");
        }
        if (!order.getTechnicianId().equals(loginUser.getUser().getId())) {
            return Result.error("无权操作此工单");
        }
        if (order.getStatus() != 2) {
            return Result.error("工单状态不允许申领配件");
        }
        
        // 验证配件库存
        BizPart part = partMapper.selectById(partId);
        if (part == null) {
            return Result.error("配件不存在");
        }
        int available = part.getStockQuantity() - part.getReservedQuantity();
        if (available < quantity) {
            return Result.error("配件库存不足，当前可用: " + available);
        }
        
        // 扣减库存
        int beforeQty = part.getStockQuantity();
        part.setStockQuantity(beforeQty - quantity);
        partMapper.updateById(part);
        
        // 记录库存流水
        BizInventoryRecord record = new BizInventoryRecord();
        record.setPartId(partId);
        record.setRecordType(2); // 出库
        record.setQuantity(quantity);
        record.setBeforeQuantity(beforeQty);
        record.setAfterQuantity(part.getStockQuantity());
        record.setRelatedOrderId(orderId);
        record.setOperatorId(loginUser.getUser().getId());
        record.setRemark("维修申领");
        inventoryRecordMapper.insert(record);
        
        // 添加工单配件记录
        BizOrderPart orderPart = new BizOrderPart();
        orderPart.setOrderId(orderId);
        orderPart.setPartId(partId);
        orderPart.setPartName(part.getPartName());
        orderPart.setQuantity(quantity);
        orderPart.setUnitPrice(part.getSalePrice());
        orderPart.setTotalPrice(part.getSalePrice().multiply(BigDecimal.valueOf(quantity)));
        orderPartMapper.insert(orderPart);
        
        // 更新工单配件费用
        BigDecimal partsCost = order.getPartsCost() != null ? order.getPartsCost() : BigDecimal.ZERO;
        order.setPartsCost(partsCost.add(orderPart.getTotalPrice()));
        order.setTotalAmount(order.getLaborCost().add(order.getPartsCost()));
        orderMapper.updateById(order);
        
        return Result.success("配件申领成功");
    }
}
