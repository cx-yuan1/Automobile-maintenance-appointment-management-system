package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizVehicle;
import com.carrepair.mapper.BizVehicleMapper;
import com.carrepair.service.BizVehicleService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BizVehicleServiceImpl extends ServiceImpl<BizVehicleMapper, BizVehicle> implements BizVehicleService {

    @Override
    public List<BizVehicle> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<BizVehicle>()
                .eq(BizVehicle::getUserId, userId)
                .eq(BizVehicle::getStatus, 1)
                .orderByDesc(BizVehicle::getCreateTime));
    }
}
