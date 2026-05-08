package com.wuyou.rag.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.rag.audit.AuditLogService;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.entity.sys.SysUser;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbAuditLogMapper;
import com.wuyou.rag.mapper.KbChatHistoryMapper;
import com.wuyou.rag.mapper.KbConfigMapper;
import com.wuyou.rag.mapper.KbDocumentMapper;
import com.wuyou.rag.mapper.KbKnowledgeBaseMapper;
import com.wuyou.rag.mapper.SysUserMapper;
import com.wuyou.rag.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理后台")
public class AdminController {

    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbChatHistoryMapper kbChatHistoryMapper;
    private final SysUserMapper sysUserMapper;
    private final KbConfigMapper kbConfigMapper;
    private final KbAuditLogMapper kbAuditLogMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsResponse> stats() {
        long totalKnowledgeBases = kbKnowledgeBaseMapper.selectCount(null);
        long totalDocuments = kbDocumentMapper.selectCount(null);
        long totalChats = kbChatHistoryMapper.selectCount(null);
        long totalUsers = sysUserMapper.selectCount(null);
        return Result.success(new DashboardStatsResponse(
                totalKnowledgeBases, totalDocuments, totalChats, totalUsers));
    }

    @GetMapping("/audit-logs")
    public Result<?> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return auditLogService.page(page, size, operation, startTime, endTime);
    }

    @GetMapping("/config")
    public Result<List<KbConfig>> listConfigs() {
        List<KbConfig> configs = kbConfigMapper.selectList(null);
        return Result.success(configs);
    }

    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody @Valid UpdateConfigRequest request) {
        KbConfig config = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, request.getConfigKey()));
        if (config == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "配置项不存在");
        }
        config.setConfigValue(request.getConfigValue());
        kbConfigMapper.updateById(config);
        return Result.success(null);
    }

    @GetMapping("/users")
    public Result<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(sysUserMapper.selectPage(new Page<>(page, size), null));
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        sysUserMapper.updateById(user);
        return Result.success(null);
    }

    @PostMapping("/users")
    public Result<?> createUser(@RequestBody @Valid CreateUserRequest request) {
        SysUser existing = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (existing != null) {
            return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        return Result.success(user);
    }

    // ---- DTOs ----

    @Data
    @AllArgsConstructor
    public static class DashboardStatsResponse {
        private long totalKnowledgeBases;
        private long totalDocuments;
        private long totalChats;
        private long totalUsers;
    }

    @Data
    public static class UpdateConfigRequest {
        @NotBlank(message = "配置键不能为空")
        private String configKey;

        @NotBlank(message = "配置值不能为空")
        private String configValue;
    }

    @Data
    public static class UpdateUserRequest {
        private String role;
        private Integer status;
        private String password;
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String nickname;
        private String role;
    }
}
