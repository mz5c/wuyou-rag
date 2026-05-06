package com.wuyou.rag.result;

import com.wuyou.rag.constant.ApiConstant;
import lombok.Getter;

@Getter
public class Result<T> {
    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ApiConstant.CODE_SUCCESS, ApiConstant.MSG_SUCCESS, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
