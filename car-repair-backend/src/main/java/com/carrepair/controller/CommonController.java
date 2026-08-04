package com.carrepair.controller;

import com.carrepair.common.Result;
import com.carrepair.entity.SysUser;
import com.carrepair.security.LoginUser;
import com.carrepair.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 公共接口控制器
 */
@CrossOrigin
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private SysUserService sysUserService;

    /** 文件上传路径 */
    @Value("${file.upload-path:uploads}")
    private String uploadPath;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/currentUser")
    public Result<Map<String, Object>> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Result.error(401, "未登录");
        }
        
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        SysUser user = loginUser.getUser();
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("userType", user.getUserType());
        data.put("phone", user.getPhone());
        data.put("customerLevel", user.getCustomerLevel());
        
        return Result.success(data);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        // 检查用户名是否存在
        if (sysUserService.getByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        // 检查手机号是否存在
        if (sysUserService.getByPhone(user.getPhone()) != null) {
            return Result.error("手机号已被注册");
        }
        
        if (sysUserService.register(user)) {
            return Result.success("注册成功");
        }
        return Result.error("注册失败");
    }

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        
        // 生成新文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;
        
        // 创建上传目录
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if(!created) {
                return Result.error("创建上传目录失败: " + uploadDir.getAbsolutePath());
            }
        }
        System.out.println("Uploading file to: " + uploadDir.getAbsolutePath());
        
        // 保存文件
        File destFile = new File(uploadDir, newFilename);
        try {
            file.transferTo(destFile);
            // 返回访问路径
            String url = "/uploads/" + newFilename;
            return Result.success("上传成功", url);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
