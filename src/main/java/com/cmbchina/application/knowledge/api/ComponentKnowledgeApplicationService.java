package com.cmbchina.application.knowledge.api;
import com.cmbchina.application.knowledge.api.dto.PageComponentsRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentNameResponse;
import java.util.List;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeResponse;
/** 组件上下文查询边界。 */
public interface ComponentKnowledgeApplicationService {
    /** 获取组件及接口知识。 */
    ComponentKnowledgeResponse query(ComponentKnowledgeRequest request);

    /** 查询指定页面的组件名称。 */
    List<ComponentNameResponse> pageComponents(PageComponentsRequest request);
}