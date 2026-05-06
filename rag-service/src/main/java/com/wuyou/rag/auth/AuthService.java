package com.wuyou.rag.auth;

import com.wuyou.rag.result.Result;

public interface AuthService {

    record LoginResponse(String token, Long userId, String username, String nickname, String role) {}

    record UserInfo(Long userId, String username, String nickname, String role, Integer status) {}

    Result<LoginResponse> login(String username, String password);

    void logout(String token);

    Result<UserInfo> getCurrentUser(Long userId);

    Result<Void> register(String username, String password, String nickname, Long operatorId);
}
