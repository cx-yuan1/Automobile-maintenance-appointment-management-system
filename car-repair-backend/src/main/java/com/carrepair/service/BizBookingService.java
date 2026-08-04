package com.carrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizBooking;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BizBookingService extends IService<BizBooking> {
    
    /** 获取可预约时间槽 */
    List<Map<String, Object>> getTimeSlots(LocalDate date);
    
    /** 创建预约 */
    BizBooking createBooking(BizBooking booking, List<Long> serviceIds);
    
    /** 获取用户预约列表 */
    IPage<BizBooking> pageByUserId(Long userId, Integer status, Integer page, Integer size);
    
    /** 管理端分页查询 */
    IPage<BizBooking> pageList(Integer status, LocalDate date, Integer page, Integer size);
    
    /** 确认预约 */
    boolean confirm(Long id);
    
    /** 取消预约 */
    boolean cancel(Long id);
}
