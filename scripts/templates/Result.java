package template.base.application.api.dto;

import template.base.infrastructure.common.ResponseEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * 统一响应，HTTP 状态为 200，业务结果由 code 和 success 表示。
 *
 * @param <T>
 *            业务数据类型
 */
@Getter
@Builder
public final class Result<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    /** 返回业务数据的成功响应。 */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(ResponseEnum.SUCCESS.getCode()).message(ResponseEnum.SUCCESS.getMessage()).data(data).success(true).build();
    }

    /** 返回指定错误的失败响应。 */
    public static <T> Result<T> failure(int code, String message, T data) {
        return Result.<T>builder().code(code).message(message).data(data).success(false).build();
    }

    /** 返回统一错误枚举对应的失败响应。 */
    public static <T> Result<T> failure(ResponseEnum error, T data) {
        return failure(error.getCode(), error.getMessage(), data);
    }
}
