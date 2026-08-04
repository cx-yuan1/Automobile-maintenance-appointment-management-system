package com.carrepair.security;

import com.carrepair.entity.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security 登录用户
 */
@Data
public class LoginUser implements UserDetails {

    private SysUser user;

    public LoginUser(SysUser user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 根据用户类型设置角色
        switch (user.getUserType()) {
            case 1:
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                break;
            case 2:
                authorities.add(new SimpleGrantedAuthority("ROLE_TECHNICIAN"));
                break;
            case 3:
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                break;
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}
