package com.wuyou.rag.controller;

import com.wuyou.rag.auth.AuthService;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthService.LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail(ErrorCode.UNAUTHORIZED.getCode(), "无效的令牌");
        }
        authService.logout(token);
        return Result.success(null);
    }

    @GetMapping("/userinfo")
    public Result<AuthService.UserInfo> userinfo(@AuthenticationPrincipal Long userId) {
        return authService.getCurrentUser(userId);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request,
                                  @AuthenticationPrincipal Long userId) {
        return authService.register(request.getUsername(), request.getPassword(),
                request.getNickname(), userId);
    }

    private String extractToken(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String nickname;
    }
}
