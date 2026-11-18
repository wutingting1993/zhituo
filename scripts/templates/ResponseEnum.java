package template.base.infrastructure.common;

/** 统一错误码与默认文案。 */
public enum ResponseEnum {
    SUCCESS(200, "成功"),
    VALIDATION_ERROR(10000, "参数校验失败"),
    BUSINESS_ERROR(10001, "业务处理失败"),
    NOT_FOUND(10002, "请求资源不存在"),
    METHOD_NOT_ALLOWED(10003, "不支持的请求方法"),
    MEDIA_TYPE_NOT_SUPPORTED(10004, "不支持的请求内容类型"),
    UPLOAD_TOO_LARGE(10005, "上传文件超过大小限制"),
    UNKNOWN_ERROR(99999, "处理失败，请稍后重试");

    private final int code;
    private final String message;

    /** 保存错误码与可对外展示的默认文案。 */
    ResponseEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 返回错误码。 */
    public int getCode() {
        return code;
    }

    /** 返回默认文案。 */
    public String getMessage() {
        return message;
    }
}
