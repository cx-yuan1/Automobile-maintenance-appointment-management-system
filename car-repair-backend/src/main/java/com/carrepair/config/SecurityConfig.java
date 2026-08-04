package com.carrepair.config;

import com.alibaba.fastjson2.JSON;
import com.carrepair.common.Result;
import com.carrepair.entity.SysUser;
import com.carrepair.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security 配置
 * 只做权限控制，不做密码加密
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 密码编码器 - 使用明文（不加密）
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 关闭CSRF
            .csrf().disable()
            // 启用跨域
            .cors()
            .and()
            // 权限配置
            .authorizeRequests()
                // 登录注册接口放行
                .antMatchers("/api/common/**").permitAll()
                // WebSocket放行
                .antMatchers("/ws/**").permitAll()
                // 静态资源放行
                .antMatchers("/uploads/**").permitAll()
                // 前台接口 - 需要客户角色
                .antMatchers("/api/front/**").hasAnyRole("CUSTOMER", "ADMIN")
                // 维修端接口 - 需要维修人员角色
                .antMatchers("/api/technician/**").hasAnyRole("TECHNICIAN", "ADMIN")
                // 管理端接口 - 需要管理员角色
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他请求需要认证
                .anyRequest().authenticated()
            .and()
            // 表单登录配置
            .formLogin()
                .loginProcessingUrl("/api/common/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            .and()
            // 登出配置
            .logout()
                .logoutUrl("/api/common/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .permitAll()
            .and()
            // 异常处理
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    PrintWriter writer = response.getWriter();
                    writer.write(JSON.toJSONString(Result.error(401, "请先登录")));
                    writer.flush();
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    PrintWriter writer = response.getWriter();
                    writer.write(JSON.toJSONString(Result.error(403, "权限不足")));
                    writer.flush();
                })
            .and()
            // 使用Session
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .and()
            // 禁用HTTP Basic认证，避免WebSocket握手时弹出浏览器认证框
            .httpBasic().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }

    /**
     * 登录成功处理器
     * 验证用户选择的角色是否与数据库中的角色匹配
     */
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            SysUser user = loginUser.getUser();
            
            // 获取前端传递的用户类型
            String userTypeParam = request.getParameter("userType");
            if (userTypeParam != null) {
                int selectedUserType = Integer.parseInt(userTypeParam);
                // 验证选择的角色是否与数据库中的角色匹配
                if (user.getUserType() != selectedUserType) {
                    // 角色不匹配，清除认证信息
                    request.getSession().invalidate();
                    writer.write(JSON.toJSONString(Result.error("登录身份与账号不匹配，请选择正确的身份")));
                    writer.flush();
                    return;
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("userType", user.getUserType());
            data.put("phone", user.getPhone());
            
            writer.write(JSON.toJSONString(Result.success("登录成功", data)));
            writer.flush();
        };
    }

    /**
     * 登录失败处理器
     */
    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(JSON.toJSONString(Result.error("用户名或密码错误")));
            writer.flush();
        };
    }

    /**
     * 登出成功处理器
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(JSON.toJSONString(Result.success("登出成功")));
            writer.flush();
        };
    }
}
