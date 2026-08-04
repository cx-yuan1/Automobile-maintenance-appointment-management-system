package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.SysUser;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-员工管理控制器
 */
@RestController
@RequestMapping("/api/admin/employee")
public class AdminEmployeeController {

    @Autowired
    private SysUserService userService;
    
    @Autowired
    private BizRepairOrderMapper repairOrderMapper;

    /**
     * 获取员工列表
     */
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(required = false) Integer userType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        } else {
            wrapper.in(SysUser::getUserType, 2, 3); // 维修人员和管理员
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        IPage<SysUser> pageResult = userService.page(new Page<>(page, size), wrapper);
        // 隐藏密码
        pageResult.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 获取维修人员列表（用于分配工单）
     */
    @GetMapping("/technicians")
    public Result<List<SysUser>> technicians() {
        List<SysUser> list = userService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, 2)
                .eq(SysUser::getStatus, 1));
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    /**
     * 添加员工
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysUser user) {
        // 检查用户名是否存在
        if (userService.getByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        // 检查手机号是否存在
        if (userService.getByPhone(user.getPhone()) != null) {
            return Result.error("手机号已被使用");
        }
        
        user.setStatus(1);
        if (userService.save(user)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    /**
     * 编辑员工
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysUser user) {
        // 不允许修改密码为空
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(null); // 不更新密码
        }
        if (userService.updateById(user)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有关联工单
        long count = repairOrderMapper.selectCount(
                new LambdaQueryWrapper<BizRepairOrder>().eq(BizRepairOrder::getTechnicianId, id));
        if (count > 0) {
            return Result.error("该员工有关联的维修工单，无法删除。如需停用请修改状态。");
        }
        
        if (userService.removeById(id)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }
}
