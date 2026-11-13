package com.cmbchina.application.knowledge.api;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeResponse;
/** 组件上下文查询边界。 */
public interface ComponentKnowledgeApplicationService {
    /** 获取组件及接口知识。 */
    ComponentKnowledgeResponse query(ComponentKnowledgeRequest request);
}