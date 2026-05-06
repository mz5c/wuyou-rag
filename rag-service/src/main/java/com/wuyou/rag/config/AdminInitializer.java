package com.wuyou.rag.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.sys.SysUser;
import com.wuyou.rag.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Long adminCount = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getRole, "ADMIN"));
        if (adminCount > 0) {
            log.info("Admin user already exists, skipping initialization");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("wuyou"));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        sysUserMapper.insert(admin);
        log.info("Default admin user created: admin");
    }
}
