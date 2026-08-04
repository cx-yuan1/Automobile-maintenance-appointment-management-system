package com.carrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    
    /** 根据用户名查询 */
    SysUser getByUsername(String username);
    
    /** 根据手机号查询 */
    SysUser getByPhone(String phone);
    
    /** 用户注册 */
    boolean register(SysUser user);
}
