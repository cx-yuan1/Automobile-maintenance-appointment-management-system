package com.carrepair.service.impl;

import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import com.carrepair.service.MessageService;
import com.carrepair.websocket.RepairWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息服务实现类
 * 负责在关键业务节点向用户发送通知消息
 */
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private SysMessageMapper messageMapper;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private BizVehicleMapper vehicleMapper;
    
    @Autowired
    private RepairWebSocketHandler webSocketHandler;

    /**
     * 发送预约确认消息
     */
    @Override
    public void sendBookingConfirmedMessage(BizBooking booking) {
        // 给车主发送消息
        String customerTitle = "预约确认通知";
        String customerContent = String.format("您的预约已确认！预约编号：%s，预约时间：%s %s，请准时到店。",
                booking.getBookingNo(),
                booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                booking.getTimeSlot());
        sendMessage(booking.getUserId(), customerTitle, customerContent);
        
        // 如果指定了维修师，给维修师发送消息
        if (booking.getTechnicianId() != null) {
            String techTitle = "新预约通知";
            String techContent = String.format("您有新的预约工单！预约编号：%s，预约时间：%s %s，请做好准备。",
                    booking.getBookingNo(),
                    booking.getBookingDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    booking.getTimeSlot());
            sendMessage(booking.getTechnicianId(), techTitle, techContent);
        }
    }

    /**
     * 发送预约取消消息
     */
    @Override
    public void sendBookingCancelledMessage(BizBooking booking) {
        String title = "预约取消通知";
        String content = String.format("您的预约已取消。预约编号：%s，如有疑问请联系客服。",
                booking.getBookingNo());
        sendMessage(booking.getUserId(), title, content);
    }

    /**
     * 发送维修开始消息
     */
    @Override
    public void sendRepairStartedMessage(BizRepairOrder order) {
        // 获取车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        String vehicleInfo = vehicle != null 
                ? vehicle.getPlateNumber() + " " + vehicle.getBrand() + " " + vehicle.getModel()
                : "您的车辆";
        
        // 获取维修师信息
        String technicianName = "维修师";
        if (order.getTechnicianId() != null) {
            SysUser technician = userMapper.selectById(order.getTechnicianId());
            if (technician != null) {
                technicianName = technician.getRealName() != null ? technician.getRealName() : technician.getUsername();
            }
        }
        
        String title = "维修开始通知";
        String content = String.format("%s已开始维修，工单编号：%s，维修师：%s，我们会尽快完成维修。",
                vehicleInfo, order.getOrderNo(), technicianName);
        sendMessage(order.getUserId(), title, content);
    }

    /**
     * 发送维修完成消息
     */
    @Override
    public void sendRepairCompletedMessage(BizRepairOrder order) {
        // 获取车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        String vehicleInfo = vehicle != null 
                ? vehicle.getPlateNumber() + " " + vehicle.getBrand() + " " + vehicle.getModel()
                : "您的车辆";
        
        String title = "维修完成通知";
        String content;
        
        // 根据是否需要质检发送不同的消息
        if (order.getNeedQualityCheck() != null && order.getNeedQualityCheck() == 1) {
            content = String.format("%s维修已完成，正在进行质检！工单编号：%s，总金额：¥%.2f，质检通过后即可结算取车。",
                    vehicleInfo, order.getOrderNo(), order.getTotalAmount());
        } else {
            content = String.format("%s维修已完成！工单编号：%s，总金额：¥%.2f，请及时到店结算取车。",
                    vehicleInfo, order.getOrderNo(), order.getTotalAmount());
        }
        
        sendMessage(order.getUserId(), title, content);
    }
    
    /**
     * 发送质检通过消息
     */
    @Override
    public void sendQualityCheckPassMessage(BizRepairOrder order) {
        // 获取车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        String vehicleInfo = vehicle != null 
                ? vehicle.getPlateNumber() + " " + vehicle.getBrand() + " " + vehicle.getModel()
                : "您的车辆";
        
        String title = "质检通过通知";
        String content = String.format("%s质检已通过！工单编号：%s，总金额：¥%.2f，请及时到店结算取车。",
                vehicleInfo, order.getOrderNo(), order.getTotalAmount());
        sendMessage(order.getUserId(), title, content);
    }

    /**
     * 发送工单结算完成消息
     */
    @Override
    public void sendOrderSettledMessage(BizRepairOrder order) {
        // 获取车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(order.getVehicleId());
        String vehicleInfo = vehicle != null 
                ? vehicle.getPlateNumber()
                : "您的车辆";
        
        String title = "结算完成通知";
        String content = String.format("工单结算完成！工单编号：%s，车牌号：%s，支付金额：¥%.2f，感谢您的信任，欢迎再次光临！",
                order.getOrderNo(), vehicleInfo, order.getTotalAmount());
        sendMessage(order.getUserId(), title, content);
    }

    /**
     * 发送配件到货提醒
     */
    @Override
    public void sendPartArrivedMessage(Long userId, String partName, String orderNo) {
        String title = "配件到货通知";
        String content = String.format("您预订的配件【%s】已到货，工单编号：%s，我们将尽快为您安装。",
                partName, orderNo);
        sendMessage(userId, title, content);
    }

    /**
     * 发送通用消息
     */
    @Override
    public void sendMessage(Long userId, String title, String content) {
        System.out.println("=== 开始发送消息 ===");
        System.out.println("目标用户ID: " + userId);
        System.out.println("消息标题: " + title);
        System.out.println("消息内容: " + content);
        
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(1); // 1-系统通知
        message.setIsRead(0);
        messageMapper.insert(message);
        
        System.out.println("消息已保存到数据库,ID: " + message.getId());
        
        // 通过WebSocket实时推送消息
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "NEW_MESSAGE");
        wsMessage.put("id", message.getId());
        wsMessage.put("title", title);
        wsMessage.put("content", content);
        wsMessage.put("createTime", message.getCreateTime());
        
        System.out.println("准备通过WebSocket推送消息...");
        System.out.println("WebSocket消息内容: " + wsMessage);
        
        webSocketHandler.sendToUser(userId.toString(), wsMessage);
        
        System.out.println("=== 消息发送完成 ===");
    }
}
