package com.carrepair.service;

import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 动态定价引擎
 */
@Service
public class PricingEngine {

    @Autowired
    private BizPricingConfigMapper pricingConfigMapper;
    @Autowired
    private BizServiceItemMapper serviceItemMapper;
    @Autowired
    private BizVehicleMapper vehicleMapper;
    @Autowired
    private SysUserMapper userMapper;

    /**
     * 计算智能报价
     */
    public Map<String, Object> calculateQuotation(Long vehicleId, List<Long> serviceIds, LocalDate bookingDate) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取车辆信息
        BizVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            result.put("error", "车辆不存在");
            return result;
        }
        
        // 获取用户信息
        SysUser user = userMapper.selectById(vehicle.getUserId());
        
        // 计算基础工时费
        BigDecimal laborCost = BigDecimal.ZERO;
        for (Long serviceId : serviceIds) {
            BizServiceItem service = serviceItemMapper.selectById(serviceId);
            if (service != null) {
                laborCost = laborCost.add(service.getBasePrice());
            }
        }
        
        // 获取各类系数
        BigDecimal vehicleFactor = getVehicleFactor(vehicle.getVehicleType());
        BigDecimal seasonFactor = getSeasonFactor(bookingDate);
        BigDecimal vipDiscount = getVipDiscount(user != null ? user.getCustomerLevel() : 1);
        
        // 计算最终价格
        BigDecimal finalPrice = laborCost
                .multiply(vehicleFactor)
                .multiply(seasonFactor)
                .multiply(vipDiscount)
                .setScale(2, RoundingMode.HALF_UP);
        
        result.put("laborCost", laborCost);
        result.put("partsCost", BigDecimal.ZERO); // 配件费需要单独计算
        result.put("vehicleFactor", vehicleFactor);
        result.put("seasonFactor", seasonFactor);
        result.put("vipDiscount", vipDiscount);
        result.put("finalPrice", finalPrice);
        
        return result;
    }

    /**
     * 获取车型系数
     */
    private BigDecimal getVehicleFactor(Integer vehicleType) {
        String key;
        switch (vehicleType) {
            case 2: key = "LUXURY"; break;
            case 3: key = "NEW_ENERGY"; break;
            default: key = "NORMAL";
        }
        return getFactorValue("VEHICLE_TYPE", key, BigDecimal.ONE);
    }

    /**
     * 获取季节系数
     */
    private BigDecimal getSeasonFactor(LocalDate date) {
        int month = date.getMonthValue();
        String key;
        if (month >= 3 && month <= 5) {
            key = "SPRING";
        } else if (month >= 6 && month <= 8) {
            key = "SUMMER";
        } else if (month >= 9 && month <= 11) {
            key = "AUTUMN";
        } else {
            key = "WINTER";
        }
        return getFactorValue("SEASON", key, BigDecimal.ONE);
    }

    /**
     * 获取VIP折扣
     */
    private BigDecimal getVipDiscount(Integer customerLevel) {
        String key;
        switch (customerLevel) {
            case 2: key = "SILVER"; break;
            case 3: key = "GOLD"; break;
            case 4: key = "DIAMOND"; break;
            default: key = "NORMAL";
        }
        return getFactorValue("VIP_LEVEL", key, BigDecimal.ONE);
    }

    /**
     * 从数据库获取系数值
     */
    private BigDecimal getFactorValue(String configType, String configKey, BigDecimal defaultValue) {
        BizPricingConfig config = pricingConfigMapper.selectOne(
                new LambdaQueryWrapper<BizPricingConfig>()
                        .eq(BizPricingConfig::getConfigType, configType)
                        .eq(BizPricingConfig::getConfigKey, configKey)
                        .eq(BizPricingConfig::getStatus, 1));
        return config != null ? config.getFactorValue() : defaultValue;
    }
}
