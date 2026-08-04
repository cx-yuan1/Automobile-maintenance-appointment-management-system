package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizBookingServiceItem;
import com.carrepair.entity.BizOrderService;
import com.carrepair.entity.BizServiceItem;
import com.carrepair.mapper.BizBookingServiceMapper;
import com.carrepair.mapper.BizOrderServiceMapper;
import com.carrepair.service.BizServiceItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-服务项目管理控制器
 */
@RestController
@RequestMapping("/api/admin/service")
public class AdminServiceController {

    @Autowired
    private BizServiceItemService serviceItemService;
    
    @Autowired
    private BizBookingServiceMapper bookingServiceMapper;
    
    @Autowired
    private BizOrderServiceMapper orderServiceMapper;

    /**
     * 获取服务项目列表
     */
    @GetMapping("/list")
    public Result<PageResult<BizServiceItem>> list(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<BizServiceItem> pageResult = serviceItemService.pageList(page, size, serviceName, category, status);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 添加服务项目
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody BizServiceItem serviceItem) {
        serviceItem.setStatus(1);
        if (serviceItemService.save(serviceItem)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    /**
     * 编辑服务项目
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody BizServiceItem serviceItem) {
        if (serviceItemService.updateById(serviceItem)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 删除服务项目
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否被预约引用
        long bookingCount = bookingServiceMapper.selectCount(
                new LambdaQueryWrapper<BizBookingServiceItem>().eq(BizBookingServiceItem::getServiceId, id));
        if (bookingCount > 0) {
            return Result.error("该服务项目已被预约使用，无法删除。如需停用请修改状态。");
        }
        
        // 检查是否被工单引用
        long orderCount = orderServiceMapper.selectCount(
                new LambdaQueryWrapper<BizOrderService>().eq(BizOrderService::getServiceId, id));
        if (orderCount > 0) {
            return Result.error("该服务项目已被工单使用，无法删除。如需停用请修改状态。");
        }
        
        if (serviceItemService.removeById(id)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }
}
