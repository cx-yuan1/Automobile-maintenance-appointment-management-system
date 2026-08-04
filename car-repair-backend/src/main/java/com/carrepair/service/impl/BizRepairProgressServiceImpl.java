package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.BizRepairProgress;
import com.carrepair.entity.SysMessage;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.mapper.BizRepairProgressMapper;
import com.carrepair.mapper.SysMessageMapper;
import com.carrepair.service.BizRepairProgressService;
import com.carrepair.websocket.RepairWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修进度服务实现类
 */
@Service
public class BizRepairProgressServiceImpl extends ServiceImpl<BizRepairProgressMapper, BizRepairProgress> implements BizRepairProgressService {

    @Autowired
    private BizRepairOrderMapper orderMapper;

    @Autowired
    private RepairWebSocketHandler webSocketHandler;
    
    @Autowired
    private SysMessageMapper messageMapper;

    @Override
    public List<BizRepairProgress> listByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<BizRepairProgress>()
                .eq(BizRepairProgress::getOrderId, orderId)
                .orderByDesc(BizRepairProgress::getCreateTime));
    }

    @Override
    public boolean addProgress(BizRepairProgress progress) {
        // 保存进度记录
        boolean saved = save(progress);
        
        if (saved) {
            // 获取工单信息，找到对应的客户ID
            BizRepairOrder order = orderMapper.selectById(progress.getOrderId());
            if (order != null && order.getUserId() != null) {
                // 构建推送消息
                Map<String, Object> message = new HashMap<>();
                message.put("type", "PROGRESS_UPDATE");
                message.put("orderId", progress.getOrderId());
                message.put("orderNo", order.getOrderNo());
                message.put("status", progress.getProgressStatus());
                message.put("description", progress.getProgressDesc());
                message.put("operatorName", progress.getOperatorName());
                message.put("images", progress.getImages());
                message.put("createTime", LocalDateTime.now().toString());
                
                // 通过 WebSocket 推送给客户
                webSocketHandler.sendToUser(order.getUserId().toString(), message);
                
                // 同时保存到消息表，确保用户离线时也能收到通知
                SysMessage sysMessage = new SysMessage();
                sysMessage.setUserId(order.getUserId());
                sysMessage.setTitle("维修进度更新");
                sysMessage.setContent("您的工单 " + order.getOrderNo() + " 有新进度：" + progress.getProgressStatus() + 
                        (progress.getProgressDesc() != null ? " - " + progress.getProgressDesc() : ""));
                sysMessage.setMsgType(1); // 系统通知
                sysMessage.setIsRead(0);
                messageMapper.insert(sysMessage);
            }
        }
        
        return saved;
    }
}
