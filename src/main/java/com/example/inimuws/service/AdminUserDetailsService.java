package com.example.inimuws.service;

import com.example.inimuws.entity.AdminUser;
import com.example.inimuws.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("管理者ユーザーが見つかりません"));
        return User.withUsername(adminUser.getEmail())
                .password(adminUser.getPasswordHash())
                .roles(adminUser.getRole())
                .disabled(!adminUser.isEnabled())
                .build();
    }
}
