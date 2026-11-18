package template.base.adapter.web.advice;

import template.base.application.api.dto.Result;
import template.base.infrastructure.common.ResponseEnum;
import template.base.infrastructure.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/** 统一处理应用可控制的 MVC 异常，所有响应使用 HTTP 200。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 返回业务异常，保留原始异常链。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException exception) {
        warn(exception.getMessage(), exception);
        return failure(exception.getCode(), exception.getMessage());
    }

    /** 提取请求体和表单字段校验消息。 */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Result<Void>> binding(BindException exception) {
        String details = exception.getBindingResult().getAllErrors().stream().map(
                error -> error instanceof FieldError ? ((FieldError) error).getField() + "：" + error.getDefaultMessage() : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        return invalid(details, exception);
    }

    /** 提取方法参数约束及校验消息。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> constraint(ConstraintViolationException exception) {
        String details = exception.getConstraintViolations().stream().map(error -> error.getPropertyPath() + "：" + error.getMessage()).sorted()
                .collect(Collectors.joining("；"));
        return invalid(details, exception);
    }

    /** 标明缺失参数及预期类型。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> missingParameter(MissingServletRequestParameterException exception) {
        return invalid(exception.getParameterName() + "：必填，类型为 " + exception.getParameterType(), exception);
    }

    /** 标明转换失败的参数及目标类型。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        Class<?> requiredType = exception.getRequiredType();
        String type = requiredType == null ? "接口要求的类型" : requiredType.getSimpleName();
        return invalid(exception.getName() + "：类型应为 " + type, exception);
    }

    /** 返回实际 JSON 字段路径、类型要求或请求体语法错误。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> unreadableBody(HttpMessageNotReadableException exception) {
        return invalid(bodyMessage(exception), exception);
    }

    /** 返回缺失的上传字段名称。 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> missingPart(MissingServletRequestPartException exception) {
        return invalid(exception.getRequestPartName() + "：上传字段缺失", exception);
    }

    /** 处理上传大小超限。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return expected(ResponseEnum.UPLOAD_TOO_LARGE, exception);
    }

    /** 处理不支持的请求方法。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return expected(ResponseEnum.METHOD_NOT_ALLOWED, exception);
    }

    /** 处理不支持的请求内容类型。 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> mediaNotSupported(HttpMediaTypeNotSupportedException exception) {
        return expected(ResponseEnum.MEDIA_TYPE_NOT_SUPPORTED, exception);
    }

    /** 处理已由 MVC 抛出的路由不存在异常。 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> notFound(NoHandlerFoundException exception) {
        return expected(ResponseEnum.NOT_FOUND, exception);
    }

    /** 记录非预期异常并隐藏内部细节。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception exception) {
        Throwable original = NestedExceptionUtils.getMostSpecificCause(exception);
        log.error("非预期异常，原始错误：{}", original.getMessage(), exception);
        return failure(ResponseEnum.UNKNOWN_ERROR.getCode(), ResponseEnum.UNKNOWN_ERROR.getMessage());
    }

    /** 统一返回参数错误。 */
    private ResponseEntity<Result<Void>> invalid(String details, Exception exception) {
        warn(details, exception);
        return failure(ResponseEnum.VALIDATION_ERROR.getCode(), details);
    }

    /** 统一返回已知请求异常。 */
    private ResponseEntity<Result<Void>> expected(ResponseEnum error, Exception exception) {
        warn(error.getMessage(), exception);
        return failure(error.getCode(), error.getMessage());
    }

    /** 使用 warn 保留上下文、原始消息和完整堆栈。 */
    private void warn(String context, Exception exception) {
        Throwable original = NestedExceptionUtils.getMostSpecificCause(exception);
        log.warn("请求处理失败，上下文：{}，原始错误：{}", context, original.getMessage(), exception);
    }

    /** 使用 HTTP 200 返回数字错误码。 */
    private ResponseEntity<Result<Void>> failure(int code, String message) {
        return ResponseEntity.ok(Result.<Void>failure(code, message, null));
    }

    /** 从异常链提取可对外展示的请求体错误。 */
    private String bodyMessage(HttpMessageNotReadableException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof JsonMappingException) {
                return mappingMessage((JsonMappingException) current);
            }
            current = current.getCause();
        }
        return exception.getCause() == null ? "请求体不能为空或无法读取" : "请求体 JSON 语法不合法";
    }

    /** 提取 JSON 字段路径及预期类型，不返回输入值。 */
    private String mappingMessage(JsonMappingException exception) {
        String path = exception.getPath().stream()
                .map(reference -> reference.getFieldName() == null ? "[" + reference.getIndex() + "]" : reference.getFieldName())
                .collect(Collectors.joining("."));
        String field = path.isEmpty() ? "请求体" : path;
        if (exception instanceof InvalidFormatException) {
            Class<?> type = ((InvalidFormatException) exception).getTargetType();
            return field + "：类型应为 " + type.getSimpleName();
        }
        return field + "：结构或类型不符合接口要求";
    }
}
