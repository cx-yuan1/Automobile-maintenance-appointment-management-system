package com.carrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizPricingConfig;
import java.util.List;

public interface BizPricingConfigService extends IService<BizPricingConfig> {
    
    /** 根据类型获取配置列表 */
    List<BizPricingConfig> listByType(String configType);
}
