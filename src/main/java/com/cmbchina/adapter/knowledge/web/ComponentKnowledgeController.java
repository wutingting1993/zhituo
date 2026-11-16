package com.cmbchina.adapter.knowledge.web;
import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.knowledge.api.ComponentKnowledgeApplicationService;
import com.cmbchina.application.knowledge.api.dto.PageComponentsRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentNameResponse;
import java.util.List;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
/** 组件知识只读入口。 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class ComponentKnowledgeController {
    @Autowired
    private ComponentKnowledgeApplicationService service;
    /** 返回页面内已登记组件的标识与名称。 */
    @PostMapping("/page-components")
    public Result<List<ComponentNameResponse>> pageComponents(@Valid @RequestBody PageComponentsRequest request) {
        return Result.success(service.pageComponents(request));
    }

    /** 返回关联知识及缺口。 */
    @PostMapping("/component-context")
    public Result<ComponentKnowledgeResponse> query(@Valid @RequestBody ComponentKnowledgeRequest request) {
        return Result.success(service.query(request));
    }
}