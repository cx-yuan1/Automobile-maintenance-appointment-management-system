package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import com.carrepair.service.BizRepairOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BizRepairOrderServiceImpl extends ServiceImpl<BizRepairOrderMapper, BizRepairOrder> implements BizRepairOrderService {

    @Autowired
    private BizVehicleMapper vehicleMapper;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private BizOrderServiceMapper orderServiceMapper;
    @Autowired
    private BizOrderPartMapper orderPartMapper;
    @Autowired
    private BizRepairProgressMapper progressMapper;
    @Autowired
    private com.carrepair.service.MessageService messageService;

    @Override
    @Transactional
    public BizRepairOrder createOrder(BizRepairOrder order) {
        // 生成工单编号
        String orderNo = "WX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
        order.setOrderNo(orderNo);
        
        // 如果状态未设置，默认为待接待
        if (order.getStatus() == null) {
            order.setStatus(1);
        }
        
        // 如果金额未设置，初始化为0
        if (order.getLaborCost() == null) {
            order.setLaborCost(BigDecimal.ZERO);
        }
        if (order.getPartsCost() == null) {
            order.setPartsCost(BigDecimal.ZERO);
        }
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
        }
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(0);
        }
        
        save(order);
        return order;
    }

    @Override
    public boolean assign(Long orderId, Long technicianId) {
        BizRepairOrder order = getById(orderId);
        if (order == null) return false;
        
        order.setTechnicianId(technicianId);
        order.setStatus(2); // 维修中
        order.setStartTime(LocalDateTime.now());
        boolean success = updateById(order);
        
        // 发送维修开始消息
        if (success) {
            messageService.sendRepairStartedMessage(order);
        }
        return success;
    }

    @Override
    public Map<String, Object> getDetail(Long orderId) {
        BizRepairOrder order = getById(orderId);
        if (order == null) return null;
        
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        
        // 车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        result.put("vehicle", vehicle);
        
        // 客户信息
        SysUser customer = userMapper.selectById(order.getUserId());
        if (customer != null) {
            customer.setPassword(null); // 隐藏密码
        }
        result.put("customer", customer);
        
        // 维修人员信息
        if (order.getTechnicianId() != null) {
            SysUser technician = userMapper.selectById(order.getTechnicianId());
            if (technician != null) {
                technician.setPassword(null);
            }
            result.put("technician", technician);
        }
        
        // 服务项目
        List<BizOrderService> services = orderServiceMapper.selectList(
                new LambdaQueryWrapper<BizOrderService>().eq(BizOrderService::getOrderId, orderId));
        result.put("services", services);
        
        // 配件
        List<BizOrderPart> parts = orderPartMapper.selectList(
                new LambdaQueryWrapper<BizOrderPart>().eq(BizOrderPart::getOrderId, orderId));
        result.put("parts", parts);
        
        // 进度记录
        List<BizRepairProgress> progress = progressMapper.selectList(
                new LambdaQueryWrapper<BizRepairProgress>()
                        .eq(BizRepairProgress::getOrderId, orderId)
                        .orderByDesc(BizRepairProgress::getCreateTime));
        result.put("progress", progress);
        
        return result;
    }

    @Override
    public IPage<BizRepairOrder> pageByUserId(Long userId, Integer status, Integer page, Integer size) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRepairOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(BizRepairOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizRepairOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<BizRepairOrder> pageByTechnicianId(Long technicianId, Integer status, Integer page, Integer size) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRepairOrder::getTechnicianId, technicianId);
        if (status != null) {
            wrapper.eq(BizRepairOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizRepairOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<BizRepairOrder> pageList(String orderNo, Integer status, Long technicianId, Integer page, Integer size) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(BizRepairOrder::getOrderNo, orderNo);
        }
        if (status != null) {
            wrapper.eq(BizRepairOrder::getStatus, status);
        }
        if (technicianId != null) {
            wrapper.eq(BizRepairOrder::getTechnicianId, technicianId);
        }
        wrapper.orderByDesc(BizRepairOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }
    
    @Override
    public IPage<Map<String, Object>> pageListWithUserInfo(String orderNo, Integer status, Long technicianId, Integer page, Integer size) {
        return baseMapper.selectPageWithUserInfo(new Page<>(page, size), orderNo, status, technicianId);
    }

    @Override
    @Transactional
    public boolean complete(Long orderId, BigDecimal actualHours) {
        BizRepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != 2) return false;
        
        // 重新计算总金额（从服务项目和配件表统计）
        recalculateTotalAmount(orderId);
        
        // 重新获取更新后的工单
        order = getById(orderId);
        
        // 判断是否需要质检：检查工单关联的服务项目中是否有需要质检的
        boolean needQualityCheck = checkIfNeedQualityCheck(orderId);
        order.setNeedQualityCheck(needQualityCheck ? 1 : 0);
        
        // 根据是否需要质检设置不同的状态
        if (needQualityCheck) {
            order.setStatus(3); // 待质检
        } else {
            order.setStatus(4); // 直接进入待结算
        }
        
        order.setActualHours(actualHours);
        order.setEndTime(LocalDateTime.now());
        
        boolean success = updateById(order);
        
        // 发送维修完成消息
        if (success) {
            messageService.sendRepairCompletedMessage(order);
        }
        return success;
    }
    
    /**
     * 检查工单是否需要质检
     * 如果工单中包含任何需要质检的服务项目，则该工单需要质检
     */
    private boolean checkIfNeedQualityCheck(Long orderId) {
        // 查询工单的服务项目
        List<BizOrderService> services = orderServiceMapper.selectList(
                new LambdaQueryWrapper<BizOrderService>().eq(BizOrderService::getOrderId, orderId));
        
        if (services == null || services.isEmpty()) {
            return false;
        }
        
        // 检查是否有需要质检的服务项目
        // 这里简化处理：刹车系统、四轮定位、火花塞、钣金喷漆等需要质检
        for (BizOrderService service : services) {
            String serviceName = service.getServiceName();
            if (serviceName.contains("刹车") || serviceName.contains("四轮定位") || 
                serviceName.contains("火花塞") || serviceName.contains("钣金") || 
                serviceName.contains("喷漆")) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    @Transactional
    public boolean qualityCheckPass(Long orderId) {
        BizRepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != 3) {
            return false;
        }
        
        // 质检通过，进入待结算状态
        order.setStatus(4);
        boolean success = updateById(order);
        
        // 发送质检通过消息
        if (success) {
            messageService.sendQualityCheckPassMessage(order);
        }
        
        return success;
    }

    @Override
    public boolean pay(Long orderId) {
        BizRepairOrder order = getById(orderId);
        if (order == null || order.getPaymentStatus() == 1) return false;
        
        order.setPaymentStatus(1);
        order.setStatus(5); // 已完成
        boolean success = updateById(order);
        
        // 发送工单结算完成消息
        if (success) {
            messageService.sendOrderSettledMessage(order);
        }
        return success;
    }
    
    @Override
    @Transactional
    public void recalculateTotalAmount(Long orderId) {
        BizRepairOrder order = getById(orderId);
        if (order == null) return;
        
        // 计算服务项目总价
        List<BizOrderService> services = orderServiceMapper.selectList(
                new LambdaQueryWrapper<BizOrderService>().eq(BizOrderService::getOrderId, orderId));
        BigDecimal laborCost = services.stream()
                .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算配件总价
        List<BizOrderPart> parts = orderPartMapper.selectList(
                new LambdaQueryWrapper<BizOrderPart>().eq(BizOrderPart::getOrderId, orderId));
        BigDecimal partsCost = parts.stream()
                .map(p -> p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 更新工单金额
        order.setLaborCost(laborCost);
        order.setPartsCost(partsCost);
        order.setTotalAmount(laborCost.add(partsCost));
        updateById(order);
    }
}
