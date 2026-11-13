package com.cmbchina.adapter.knowledge.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.util.RawValue;
import javax.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 仅压缩组件上下文接口的 JSON 空字段，不改变原始知识数据。 */
@RestControllerAdvice
public class ComponentContextResponseAdvice implements ResponseBodyAdvice<Object> {
    @Autowired
    private Jackson2ObjectMapperBuilder builder;
    private ObjectMapper contextMapper;

    /** 使用 Boot 构建配置创建本接口专用 Mapper，不注册为全局 Bean。 */
    @PostConstruct
    public void initializeMapper() {
        contextMapper = builder.build();
        contextMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** 只处理 Jackson JSON 响应。 */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /** 限定组件上下文路径，异常响应也遵循相同的空字段约定。 */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return body;
        }
        String path = ((ServletServerHttpRequest) request).getServletRequest().getServletPath();
        if (!"/api/v1/knowledge/component-context".equals(path) && !"/api/v1/knowledge/component-context/".equals(path)) {
            return body;
        }
        try {
            return new RawValue(contextMapper.writeValueAsString(body));
        } catch (JsonProcessingException exception) {
            throw new HttpMessageNotWritableException("组件上下文响应序列化失败", exception);
        }
    }
}
