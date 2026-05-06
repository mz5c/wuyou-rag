package com.wuyou.rag.audit;

import com.wuyou.rag.result.Result;

import java.time.LocalDateTime;

public interface AuditLogService {

    void log(Long userId, String username, String operation, String detail, String ip, String userAgent);

    Result<?> page(int page, int size, String operation, LocalDateTime startTime, LocalDateTime endTime);
}
