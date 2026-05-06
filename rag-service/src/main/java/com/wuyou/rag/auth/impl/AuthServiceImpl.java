package com.wuyou.rag.auth.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.auth.AuthService;
import com.wuyou.rag.auth.JwtTokenProvider;
import com.wuyou.rag.entity.sys.SysUser;
import com.wuyou.rag.exception.BizException;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.SysUserMapper;
import com.wuyou.rag.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Result<LoginResponse> login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));

        if (user == null) {
            return Result.fail(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User login: username={}, role={}", username, user.getRole());

        return Result.success(new LoginResponse(
                token, user.getId(), user.getUsername(), user.getNickname(), user.getRole()));
    }

    @Override
    public void logout(String token) {
        jwtTokenProvider.blacklistToken(token);
        log.info("User logged out, token blacklisted");
    }

    @Override
    public Result<UserInfo> getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return Result.success(new UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getStatus()));
    }

    @Override
    public Result<Void> register(String username, String password, String nickname, Long operatorId) {
        Long count = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole("USER");
        user.setStatus(1);
        sysUserMapper.insert(user);

        log.info("User registered: username={}, operatorId={}", username, operatorId);
        return Result.success(null);
    }
}
