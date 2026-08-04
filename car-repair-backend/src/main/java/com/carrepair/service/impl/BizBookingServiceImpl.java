package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import com.carrepair.service.BizBookingService;
import com.carrepair.service.BizRepairOrderService;
import com.carrepair.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BizBookingServiceImpl extends ServiceImpl<BizBookingMapper, BizBooking> implements BizBookingService {

    @Autowired
    private BizBookingServiceMapper bookingServiceMapper;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private BizRepairOrderService repairOrderService;
    
    @Autowired
    private BizServiceItemMapper serviceItemMapper;
    
    @Autowired
    private BizOrderServiceMapper orderServiceMapper;

    // 时间槽配置
    private static final String[] TIME_SLOTS = {"08:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00"};
    private static final int MAX_PER_SLOT = 5;

    @Override
    public List<Map<String, Object>> getTimeSlots(LocalDate date) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String slot : TIME_SLOTS) {
            // 统计该时段已预约数量
            long count = count(new LambdaQueryWrapper<BizBooking>()
                    .eq(BizBooking::getBookingDate, date)
                    .eq(BizBooking::getTimeSlot, slot)
                    .in(BizBooking::getStatus, 1, 2, 3)); // 待确认、已确认、已到店
            
            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("timeSlot", slot);
            slotInfo.put("maxCount", MAX_PER_SLOT);
            slotInfo.put("bookedCount", count);
            slotInfo.put("available", count < MAX_PER_SLOT);
            result.add(slotInfo);
        }
        return result;
    }

    @Override
    @Transactional
    public BizBooking createBooking(BizBooking booking, List<Long> serviceIds) {
        // 生成预约编号
        String bookingNo = "BK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
        booking.setBookingNo(bookingNo);
        booking.setStatus(1); // 待确认
        save(booking);
        
        // 保存预约服务关联
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (Long serviceId : serviceIds) {
                BizBookingServiceItem bs = new BizBookingServiceItem();
                bs.setBookingId(booking.getId());
                bs.setServiceId(serviceId);
                bs.setQuantity(1);
                bookingServiceMapper.insert(bs);
            }
        }
        return booking;
    }

    @Override
    public IPage<BizBooking> pageByUserId(Long userId, Integer status, Integer page, Integer size) {
        // 使用自定义查询，关联获取维修工名称
        return baseMapper.selectPageWithTechnician(new Page<>(page, size), userId, status);
    }

    @Override
    public IPage<BizBooking> pageList(Integer status, LocalDate date, Integer page, Integer size) {
        // 使用自定义查询，关联获取车主和维修工名称
        String dateStr = date != null ? date.toString() : null;
        return baseMapper.selectPageForAdmin(new Page<>(page, size), status, dateStr);
    }

    @Override
    @Transactional
    public boolean confirm(Long id) {
        BizBooking booking = getById(id);
        if (booking == null || booking.getStatus() != 1) {
            return false;
        }
        
        // 1. 更新预约状态为已确认
        booking.setStatus(2);
        boolean success = updateById(booking);
        
        if (success) {
            // 2. 自动创建维修工单
            BizRepairOrder order = new BizRepairOrder();
            order.setBookingId(booking.getId());
            order.setUserId(booking.getUserId());
            order.setVehicleId(booking.getVehicleId());
            order.setTechnicianId(booking.getTechnicianId()); // 使用预约时选择的维修师
            order.setStatus(2); // 直接设置为维修中状态
            order.setStartTime(LocalDateTime.now()); // 设置开始时间
            order.setLaborCost(booking.getEstimatedPrice() != null ? booking.getEstimatedPrice() : BigDecimal.ZERO);
            order.setPartsCost(BigDecimal.ZERO);
            order.setTotalAmount(booking.getEstimatedPrice() != null ? booking.getEstimatedPrice() : BigDecimal.ZERO);
            order.setPaymentStatus(0); // 未支付
            
            // 创建工单
            BizRepairOrder createdOrder = repairOrderService.createOrder(order);
            
            // 3. 复制预约的服务项目到工单
            List<BizBookingServiceItem> bookingServices = bookingServiceMapper.selectList(
                    new LambdaQueryWrapper<BizBookingServiceItem>()
                            .eq(BizBookingServiceItem::getBookingId, booking.getId()));
            
            if (bookingServices != null && !bookingServices.isEmpty()) {
                for (BizBookingServiceItem bs : bookingServices) {
                    // 获取服务项目详情
                    BizServiceItem serviceItem = serviceItemMapper.selectById(bs.getServiceId());
                    
                    if (serviceItem != null) {
                        // 创建工单服务记录
                        BizOrderService orderService = new BizOrderService();
                        orderService.setOrderId(createdOrder.getId());
                        orderService.setServiceId(serviceItem.getId());
                        orderService.setServiceName(serviceItem.getServiceName());
                        orderService.setStandardHours(serviceItem.getStandardHours());
                        orderService.setUnitPrice(serviceItem.getBasePrice());
                        orderService.setTotalPrice(serviceItem.getBasePrice()
                                .multiply(BigDecimal.valueOf(bs.getQuantity())));
                        orderService.setStatus(1); // 待处理
                        
                        orderServiceMapper.insert(orderService);
                    }
                }
            }
            
            // 4. 发送预约确认消息（给车主和维修师）
            messageService.sendBookingConfirmedMessage(booking);
            
            // 5. 发送维修开始消息
            messageService.sendRepairStartedMessage(createdOrder);
        }
        
        return success;
    }

    @Override
    public boolean cancel(Long id) {
        BizBooking booking = getById(id);
        if (booking == null || booking.getStatus() > 3) {
            return false;
        }
        booking.setStatus(4); // 已取消
        boolean success = updateById(booking);
        
        // 发送预约取消消息
        if (success) {
            messageService.sendBookingCancelledMessage(booking);
        }
        return success;
    }
}
