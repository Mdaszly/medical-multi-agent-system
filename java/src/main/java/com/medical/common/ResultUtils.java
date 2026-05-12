package com.medical.common;

public class ResultUtils {

    private ResultUtils() {
    }

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.success(data);
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.success(data, message);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return BaseResponse.error(errorCode);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return BaseResponse.error(code, message);
    }
}