package com.carrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizRepairOrder;
import java.util.Map;

public interface BizRepairOrderService extends IService<BizRepairOrder> {
    
    /** 创建工单 */
    BizRepairOrder createOrder(BizRepairOrder order);
    
    /** 分配维修人员 */
    boolean assign(Long orderId, Long technicianId);
    
    /** 获取工单详情(含关联信息) */
    Map<String, Object> getDetail(Long orderId);
    
    /** 用户工单分页 */
    IPage<BizRepairOrder> pageByUserId(Long userId, Integer status, Integer page, Integer size);
    
    /** 维修人员工单分页 */
    IPage<BizRepairOrder> pageByTechnicianId(Long technicianId, Integer status, Integer page, Integer size);
    
    /** 管理端分页 */
    IPage<BizRepairOrder> pageList(String orderNo, Integer status, Long technicianId, Integer page, Integer size);
    
    /** 管理端分页（包含车主和维修师信息） */
    IPage<Map<String, Object>> pageListWithUserInfo(String orderNo, Integer status, Long technicianId, Integer page, Integer size);
    
    /** 完工确认 */
    boolean complete(Long orderId, java.math.BigDecimal actualHours);
    
    /** 质检通过 */
    boolean qualityCheckPass(Long orderId);
    
    /** 重新计算工单总金额 */
    void recalculateTotalAmount(Long orderId);
    
    /** 模拟支付 */
    boolean pay(Long orderId);
}
