package template.base.infrastructure.common.exception;

import template.base.infrastructure.common.ResponseEnum;

/** 应用、领域及基础设施可抛出的业务异常，保留原始异常链。 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;

    /** 使用统一错误枚举创建业务异常。 */
    public BusinessException(ResponseEnum error) {
        this(error.getCode(), error.getMessage(), null);
    }

    /** 包装已有异常时，必须传入原异常，保留完整异常链。 */
    public BusinessException(ResponseEnum error, Throwable cause) {
        this(error.getCode(), error.getMessage(), cause);
    }

    /** 使用错误码、可对外展示的消息及原始原因创建业务异常。 */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** 返回业务错误码。 */
    public int getCode() {
        return code;
    }
}
