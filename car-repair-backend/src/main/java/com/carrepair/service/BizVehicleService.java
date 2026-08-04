package com.carrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizVehicle;
import java.util.List;

public interface BizVehicleService extends IService<BizVehicle> {
    
    /** 获取用户车辆列表 */
    List<BizVehicle> listByUserId(Long userId);
}
