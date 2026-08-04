package com.carrepair.controller.front;

import com.carrepair.common.Result;
import com.carrepair.entity.BizServiceItem;
import com.carrepair.service.BizServiceItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台-服务项目控制器
 */
@RestController
@RequestMapping("/api/front/service")
public class FrontServiceController {

    @Autowired
    private BizServiceItemService serviceItemService;

    /**
     * 获取服务项目列表
     */
    @GetMapping("/list")
    public Result<List<BizServiceItem>> list() {
        return Result.success(serviceItemService.listEnabled());
    }
}
