package com.carrepair.service;

import com.carrepair.entity.BizBooking;
import com.carrepair.entity.BizRepairOrder;

/**
 * 消息服务接口
 * 用于向用户发送各类系统通知消息
 */
public interface MessageService {
    
    /**
     * 发送预约确认消息
     * @param booking 预约信息
     */
    void sendBookingConfirmedMessage(BizBooking booking);
    
    /**
     * 发送预约取消消息
     * @param booking 预约信息
     */
    void sendBookingCancelledMessage(BizBooking booking);
    
    /**
     * 发送维修开始消息
     * @param order 工单信息
     */
    void sendRepairStartedMessage(BizRepairOrder order);
    
    /**
     * 发送维修完成消息
     * @param order 工单信息
     */
    void sendRepairCompletedMessage(BizRepairOrder order);
    
    /**
     * 发送质检通过消息
     * @param order 工单信息
     */
    void sendQualityCheckPassMessage(BizRepairOrder order);
    
    /**
     * 发送工单结算完成消息
     * @param order 工单信息
     */
    void sendOrderSettledMessage(BizRepairOrder order);
    
    /**
     * 发送配件到货提醒
     * @param userId 用户ID
     * @param partName 配件名称
     * @param orderNo 工单编号
     */
    void sendPartArrivedMessage(Long userId, String partName, String orderNo);
    
    /**
     * 发送通用消息
     * @param userId 用户ID
     * @param title 消息标题
     * @param content 消息内容
     */
    void sendMessage(Long userId, String title, String content);
}
