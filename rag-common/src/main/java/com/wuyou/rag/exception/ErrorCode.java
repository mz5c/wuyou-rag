package com.wuyou.rag.exception;

public enum ErrorCode {
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    PARAM_ERROR(400, "参数校验失败"),
    NOT_FOUND(404, "资源不存在"),
    SENSITIVE_WORD(4001, "提问包含敏感词"),
    RATE_LIMITED(4002, "请求过于频繁，请稍后再试"),
    LLM_CIRCUIT_BROKEN(5001, "AI 服务暂不可用，请稍后再试"),
    FILE_TOO_LARGE(5002, "文件大小超过限制"),
    FILE_PARSE_ERROR(5003, "文件解析失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
