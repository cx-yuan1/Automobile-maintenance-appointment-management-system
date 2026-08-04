package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizPricingConfig;
import com.carrepair.mapper.BizPricingConfigMapper;
import com.carrepair.service.BizPricingConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class BizPricingConfigServiceImpl extends ServiceImpl<BizPricingConfigMapper, BizPricingConfig> implements BizPricingConfigService {

    @Override
    public List<BizPricingConfig> listByType(String configType) {
        LambdaQueryWrapper<BizPricingConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configType)) {
            wrapper.eq(BizPricingConfig::getConfigType, configType);
        }
        wrapper.orderByAsc(BizPricingConfig::getConfigType, BizPricingConfig::getId);
        return list(wrapper);
    }
}
