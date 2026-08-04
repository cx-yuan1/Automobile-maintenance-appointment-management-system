package com.carrepair.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrepair.common.Result;
import com.carrepair.entity.BizRepairOrder;
import com.carrepair.entity.BizVehicle;
import com.carrepair.entity.SysUser;
import com.carrepair.mapper.BizRepairOrderMapper;
import com.carrepair.mapper.BizVehicleMapper;
import com.carrepair.mapper.SysUserMapper;
import com.carrepair.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台-个人中心控制器
 */
@RestController
@RequestMapping("/api/front/profile")
public class FrontProfileController {

    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private BizVehicleMapper vehicleMapper;
    
    @Autowired
    private BizRepairOrderMapper orderMapper;

    /**
     * 获取个人统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser.getUser().getId();
        Map<String, Object> result = new HashMap<>();
        
        // 车辆数量
        long vehicleCount = vehicleMapper.selectCount(
                new LambdaQueryWrapper<BizVehicle>()
                        .eq(BizVehicle::getUserId, userId)
                        .eq(BizVehicle::getStatus, 1));
        result.put("vehicleCount", vehicleCount);
        
        // 工单数量和消费总额
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getUserId, userId)
                        .eq(BizRepairOrder::getStatus, 5)
                        .eq(BizRepairOrder::getPaymentStatus, 1));
        
        result.put("orderCount", orders.size());
        
        BigDecimal totalSpent = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalSpent", totalSpent);
        
        // 积分（简化：消费1元=1积分）
        result.put("points", totalSpent.intValue());
        
        return Result.success(result);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Map<String, String> params,
                               @AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = loginUser.getUser();
        
        String realName = params.get("realName");
        String phone = params.get("phone");
        
        if (realName != null) {
            user.setRealName(realName);
        }
        if (phone != null) {
            // 检查手机号是否被其他用户使用
            SysUser existing = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getPhone, phone)
                            .ne(SysUser::getId, user.getId()));
            if (existing != null) {
                return Result.error("该手机号已被其他用户使用");
            }
            user.setPhone(phone);
        }
        
        userMapper.updateById(user);
        return Result.success("信息更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> password(@RequestBody Map<String, String> params,
                                 @AuthenticationPrincipal LoginUser loginUser) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return Result.error("请填写完整密码信息");
        }
        
        SysUser user = loginUser.getUser();
        
        // 验证原密码（简化处理，实际应使用加密比对）
        if (!user.getPassword().equals(oldPassword)) {
            return Result.error("原密码错误");
        }
        
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }
        
        user.setPassword(newPassword);
        userMapper.updateById(user);
        
        return Result.success("密码修改成功");
    }
}
