package com.carrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizRepairProgress;
import java.util.List;

public interface BizRepairProgressService extends IService<BizRepairProgress> {
    
    /** 获取工单进度列表 */
    List<BizRepairProgress> listByOrderId(Long orderId);
    
    /** 添加进度记录 */
    boolean addProgress(BizRepairProgress progress);
}
