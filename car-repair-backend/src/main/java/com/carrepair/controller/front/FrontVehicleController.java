package com.carrepair.controller.front;

import com.carrepair.common.Result;
import com.carrepair.entity.BizVehicle;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台-车辆管理控制器
 */
@RestController
@RequestMapping("/api/front/vehicle")
public class FrontVehicleController {

    @Autowired
    private BizVehicleService vehicleService;

    /**
     * 获取当前用户车辆列表
     */
    @GetMapping("/list")
    public Result<List<BizVehicle>> list(@AuthenticationPrincipal LoginUser loginUser) {
        List<BizVehicle> list = vehicleService.listByUserId(loginUser.getUser().getId());
        return Result.success(list);
    }

    /**
     * 添加车辆
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody BizVehicle vehicle, @AuthenticationPrincipal LoginUser loginUser) {
        vehicle.setUserId(loginUser.getUser().getId());
        vehicle.setStatus(1);
        if (vehicleService.save(vehicle)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    /**
     * 编辑车辆
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody BizVehicle vehicle, @AuthenticationPrincipal LoginUser loginUser) {
        // 验证车辆归属
        BizVehicle existing = vehicleService.getById(vehicle.getId());
        if (existing == null || !existing.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("车辆不存在或无权操作");
        }
        if (vehicleService.updateById(vehicle)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 删除车辆(软删除)
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        BizVehicle existing = vehicleService.getById(id);
        if (existing == null || !existing.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("车辆不存在或无权操作");
        }
        existing.setStatus(0); // 软删除
        if (vehicleService.updateById(existing)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }
}
