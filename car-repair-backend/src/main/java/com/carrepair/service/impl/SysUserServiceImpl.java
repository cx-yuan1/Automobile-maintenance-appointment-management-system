package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.SysUser;
import com.carrepair.mapper.SysUserMapper;
import com.carrepair.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public SysUser getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
    }

    @Override
    public boolean register(SysUser user) {
        // 检查用户名是否存在
        if (getByUsername(user.getUsername()) != null) {
            return false;
        }
        // 检查手机号是否存在
        if (getByPhone(user.getPhone()) != null) {
            return false;
        }
        // 设置默认值
        user.setUserType(1); // 默认客户
        user.setCustomerLevel(1); // 默认普通会员
        user.setStatus(1); // 默认启用
        return save(user);
    }
}
