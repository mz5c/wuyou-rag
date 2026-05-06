package com.wuyou.rag.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.rag.audit.AuditLogService;
import com.wuyou.rag.entity.kb.KbAuditLog;
import com.wuyou.rag.mapper.KbAuditLogMapper;
import com.wuyou.rag.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final KbAuditLogMapper kbAuditLogMapper;

    @Override
    public void log(Long userId, String username, String operation, String detail, String ip, String userAgent) {
        KbAuditLog log = new KbAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setDetail(detail);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        kbAuditLogMapper.insert(log);
    }

    @Override
    public Result<?> page(int page, int size, String operation, LocalDateTime startTime, LocalDateTime endTime) {
        Page<KbAuditLog> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<KbAuditLog> wrapper = Wrappers.<KbAuditLog>lambdaQuery()
                .orderByDesc(KbAuditLog::getCreateTime);

        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(KbAuditLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(KbAuditLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(KbAuditLog::getCreateTime, endTime);
        }

        Page<KbAuditLog> result = kbAuditLogMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }
}
