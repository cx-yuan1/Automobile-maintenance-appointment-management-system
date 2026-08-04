package com.carrepair.controller.admin;

import com.carrepair.common.Result;
import com.carrepair.entity.BizPricingConfig;
import com.carrepair.service.BizPricingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-定价配置控制器
 */
@RestController
@RequestMapping("/api/admin/pricing")
public class AdminPricingController {

    @Autowired
    private BizPricingConfigService pricingConfigService;

    /**
     * 获取定价配置列表
     */
    @GetMapping("/list")
    public Result<List<BizPricingConfig>> list(@RequestParam(required = false) String configType) {
        return Result.success(pricingConfigService.listByType(configType));
    }

    /**
     * 更新定价配置
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody BizPricingConfig config) {
        if (pricingConfigService.updateById(config)) {
            return Result.success("配置更新成功");
        }
        return Result.error("更新失败");
    }
}
