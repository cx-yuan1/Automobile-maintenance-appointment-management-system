package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizInventoryRecord;
import com.carrepair.entity.BizOrderPart;
import com.carrepair.entity.BizPart;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.mapper.BizInventoryRecordMapper;
import com.carrepair.mapper.BizOrderPartMapper;
import com.carrepair.mapper.BizPartMapper;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.service.BizPartService;
import com.carrepair.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class BizPartServiceImpl extends ServiceImpl<BizPartMapper, BizPart> implements BizPartService {

    @Autowired
    private BizInventoryRecordMapper inventoryRecordMapper;
    
    @Autowired
    private BizOrderPartMapper orderPartMapper;
    
    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private MessageService messageService;

    @Override
    public IPage<BizPart> pageList(Integer page, Integer size, String category, String keyword, String stockStatus) {
        LambdaQueryWrapper<BizPart> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(BizPart::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BizPart::getPartName, keyword)
                    .or().like(BizPart::getPartCode, keyword));
        }
        // 库存状态筛选
        if ("low".equals(stockStatus)) {
            // 库存不足：当前库存 <= 最低库存
            wrapper.apply("stock_quantity - reserved_quantity <= min_stock");
        } else if ("normal".equals(stockStatus)) {
            // 正常：当前库存 > 最低库存
            wrapper.apply("stock_quantity - reserved_quantity > min_stock");
        }
        wrapper.orderByDesc(BizPart::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<BizPart> listWarning() {
        return list(new LambdaQueryWrapper<BizPart>()
                .eq(BizPart::getStatus, 1)
                .apply("stock_quantity - reserved_quantity <= min_stock"));
    }

    @Override
    @Transactional
    public boolean stockIn(Long partId, Integer quantity, Long operatorId, String remark) {
        BizPart part = getById(partId);
        if (part == null) return false;
        
        int before = part.getStockQuantity();
        int after = before + quantity;
        part.setStockQuantity(after);
        updateById(part);
        
        // 记录流水
        BizInventoryRecord record = new BizInventoryRecord();
        record.setPartId(partId);
        record.setRecordType(1); // 入库
        record.setQuantity(quantity);
        record.setBeforeQuantity(before);
        record.setAfterQuantity(after);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        inventoryRecordMapper.insert(record);
        
        // 检查是否有工单在等待该配件，发送到货提醒
        sendPartArrivedNotifications(partId, part.getPartName());
        
        return true;
    }
    
    /**
     * 发送配件到货提醒
     * 查找所有使用该配件且状态为维修中的工单，向客户发送到货通知
     */
    private void sendPartArrivedNotifications(Long partId, String partName) {
        // 查找使用该配件的工单配件记录
        List<BizOrderPart> orderParts = orderPartMapper.selectList(
                new LambdaQueryWrapper<BizOrderPart>().eq(BizOrderPart::getPartId, partId));
        
        if (orderParts.isEmpty()) return;
        
        // 获取相关工单ID
        List<Long> orderIds = orderParts.stream()
                .map(BizOrderPart::getOrderId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
        // 查找状态为维修中的工单
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .in(BizRepairOrder::getId, orderIds)
                        .eq(BizRepairOrder::getStatus, 2)); // 维修中
        
        // 向每个工单的客户发送到货提醒
        for (BizRepairOrder order : orders) {
            messageService.sendPartArrivedMessage(order.getUserId(), partName, order.getOrderNo());
        }
    }

    @Override
    @Transactional
    public boolean stockOut(Long partId, Integer quantity, Long operatorId, String remark) {
        BizPart part = getById(partId);
        if (part == null) return false;
        
        int available = part.getStockQuantity() - part.getReservedQuantity();
        if (available < quantity) return false;
        
        int before = part.getStockQuantity();
        int after = before - quantity;
        part.setStockQuantity(after);
        updateById(part);
        
        // 记录流水
        BizInventoryRecord record = new BizInventoryRecord();
        record.setPartId(partId);
        record.setRecordType(2); // 出库
        record.setQuantity(quantity);
        record.setBeforeQuantity(before);
        record.setAfterQuantity(after);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        inventoryRecordMapper.insert(record);
        
        return true;
    }

    @Override
    @Transactional
    public boolean reservePart(Long partId, Integer quantity, Long orderId, Long operatorId) {
        BizPart part = getById(partId);
        if (part == null) return false;
        
        // 检查可用库存
        int available = part.getStockQuantity() - part.getReservedQuantity();
        if (available < quantity) return false;
        
        // 增加预留数量
        int beforeReserved = part.getReservedQuantity();
        part.setReservedQuantity(beforeReserved + quantity);
        updateById(part);
        
        // 记录流水
        BizInventoryRecord record = new BizInventoryRecord();
        record.setPartId(partId);
        record.setRecordType(3); // 预留
        record.setQuantity(quantity);
        record.setBeforeQuantity(beforeReserved);
        record.setAfterQuantity(part.getReservedQuantity());
        record.setRelatedOrderId(orderId);
        record.setOperatorId(operatorId);
        record.setRemark("预约预留配件");
        inventoryRecordMapper.insert(record);
        
        return true;
    }

    @Override
    @Transactional
    public boolean cancelReserve(Long partId, Integer quantity, Long orderId, Long operatorId) {
        BizPart part = getById(partId);
        if (part == null) return false;
        
        // 减少预留数量
        int beforeReserved = part.getReservedQuantity();
        int newReserved = Math.max(0, beforeReserved - quantity);
        part.setReservedQuantity(newReserved);
        updateById(part);
        
        // 记录流水
        BizInventoryRecord record = new BizInventoryRecord();
        record.setPartId(partId);
        record.setRecordType(4); // 释放预留
        record.setQuantity(quantity);
        record.setBeforeQuantity(beforeReserved);
        record.setAfterQuantity(newReserved);
        record.setRelatedOrderId(orderId);
        record.setOperatorId(operatorId);
        record.setRemark("取消预约释放配件");
        inventoryRecordMapper.insert(record);
        
        return true;
    }
}
