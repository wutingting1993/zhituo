package com.cmbchina.application.modelcontext.service;
import com.cmbchina.application.modelcontext.api.ModelContextApplicationService;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentResponse;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsResponse;
import com.cmbchina.application.modelcontext.api.dto.ModelInterfaceParts;
import com.cmbchina.application.modelcontext.api.dto.ModelKnowledge;
import com.cmbchina.application.modelcontext.api.dto.ModelPage;
import com.cmbchina.application.modelcontext.api.dto.ModelComponent;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.cmbchina.application.modelcontext.api.dto.ModelRelatedGroup;
import com.cmbchina.application.modelcontext.api.dto.ModelRelatedItem;
import com.cmbchina.application.modelcontext.api.dto.ModelCodeLocation;
import com.cmbchina.application.modelcontext.support.ModelContextData;
import com.cmbchina.application.modelcontext.support.ModelContextLoader;
import com.cmbchina.application.modelcontext.support.ModelKnowledgeAssembler;
import com.cmbchina.application.modelcontext.support.ModelInterfaceAssembler;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.domain.knowledge.support.KnowledgeDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 独立组织模型上下文，不依赖展示响应。 */
@Service
public class ModelContextApplicationServiceImpl implements ModelContextApplicationService {
    @Autowired
    private ModelContextLoader loader;
    @Autowired
    private ModelKnowledgeAssembler knowledge;
    @Autowired
    private ModelInterfaceAssembler interfaces;
    @Autowired
    private KnowledgeDataSupport support;
    /** 返回完整业务和必要契约，技术知识提供目录。 */
    public ModelComponentResponse component(ModelComponentRequest request) {
        KnowledgeQuery query = query(request.getSystemCode(), request.getProjectCode(), request.getPagePath(), request.getComponentId());
        ModelContextData data = loader.load(query, request.getInterfaces(), null);
        Map<String, ModelKnowledge> entries = knowledge.collect(data.getKnowledge(), data.getIssues());
        ModelComponentResponse result = ModelComponentResponse.builder().systemCode(query.getSystemCode()).projectCode(query.getProjectCode())
                .build();
        metadata(data, result);
        Set<String> defined = fillKnowledge(data, entries, result);
        Map<String, Set<String>> parts = Maps.newLinkedHashMap();
        for (KnowledgeItem row : data.getInterfaces()) {
            parts.put(row.getId(), Sets.newHashSet("inputs", "outputs", "rules"));
        }
        result.setInterfaces(interfaces.assemble(data, parts, entries, defined, true));
        result.setIssues(knowledge.issues(data.getIssues()));
        return result;
    }

    /** 只返回明确请求的类别与接口部分。 */
    public ModelDetailsResponse details(ModelDetailsRequest request) {
        if (empty(request.getKnowledgeIds()) && empty(request.getInterfaces()) && empty(request.getInclude())) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "详情至少请求一种资料", null);
        }
        Map<String, Set<String>> parts = parts(request.getInterfaces());
        KnowledgeQuery query = query(request.getSystemCode(), request.getProjectCode(), request.getPagePath(), request.getComponentId());
        ModelContextData data = loader.load(query, null, Lists.newArrayList(parts.keySet()));
        Map<String, ModelKnowledge> entries = knowledge.collect(data.getKnowledge(), data.getIssues());
        ModelDetailsResponse result = ModelDetailsResponse.builder().build();
        List<String> requestedIds = unique(request.getKnowledgeIds());
        if (!requestedIds.isEmpty()) {
            result.setKnowledge(detailsKnowledge(requestedIds, data, entries));
        }
        if (!empty(request.getInterfaces())) {
            result.setInterfaces(interfaces.assemble(data, parts, entries, Sets.newHashSet(), false));
        }
        if (included(request, "relatedComponents")) {
            result.setRelatedComponents(related(data, requestedIds, entries));
        }
        if (included(request, "codeLocations")) {
            result.setCodeLocations(locations(data));
        }
        result.setIssues(knowledge.issues(data.getIssues()));
        return result;
    }

    private KnowledgeQuery query(String system, String project, String page, String component) {
        return KnowledgeQuery.builder().systemCode(system).projectCode(project).pagePath(page).componentId(component).build();
    }

    private void metadata(ModelContextData data, ModelComponentResponse response) {
        if (data.getPage() != null) {
            KnowledgeItem page = data.getPage();
            response.setPage(ModelPage.builder().path(page.getPath()).name(page.getName()).description(page.getDescription()).build());
        }
        if (data.getComponent() != null) {
            KnowledgeItem component = data.getComponent();
            response.setComponent(ModelComponent.builder().id(component.getId()).componentId(data.getQuery().getComponentId())
                    .name(component.getName()).description(component.getDescription()).build());
        }
    }

    private Set<String> fillKnowledge(ModelContextData data, Map<String, ModelKnowledge> entries, ModelComponentResponse response) {
        Map<String, String> types = Maps.newLinkedHashMap();
        for (KnowledgeItem row : data.getKnowledge()) {
            types.put(row.getKnowledgeId(), row.getKnowledgeType());
        }
        Set<String> defined = Sets.newHashSet();
        for (ModelKnowledge entry : entries.values()) {
            if ("BUSINESS".equals(types.get(entry.getId()))) {
                response.getBusinessKnowledge().add(entry);
                defined.add(entry.getId());
            } else {
                response.getTechnicalKnowledge()
                        .add(ModelKnowledge.builder().id(entry.getId()).title(entry.getTitle()).status(entry.getStatus()).build());
            }
        }
        return defined;
    }

    private Map<String, Set<String>> parts(List<ModelInterfaceParts> rows) {
        Map<String, Set<String>> result = Maps.newLinkedHashMap();
        if (rows == null) {
            return result;
        }
        for (ModelInterfaceParts row : rows) {
            Set<String> values = result.get(row.getId());
            if (values == null) {
                values = Sets.newLinkedHashSet();
                result.put(row.getId(), values);
            }
            values.addAll(row.getParts());
        }
        return result;
    }

    private List<ModelKnowledge> detailsKnowledge(List<String> ids, ModelContextData data, Map<String, ModelKnowledge> scoped) {
        data.getQuery().setKnowledgeIds(ids);
        List<KnowledgeItem> rows = support.knowledgeByIds(data.getQuery());
        Map<String, KnowledgeItem> found = Maps.newLinkedHashMap();
        for (KnowledgeItem row : rows) {
            found.put(row.getKnowledgeId(), row);
        }
        List<ModelKnowledge> result = Lists.newArrayList();
        for (String id : ids) {
            KnowledgeItem row = found.get(id);
            if (row == null) {
                issue(data, "KNOWLEDGE_NOT_FOUND", id);
                continue;
            }
            ModelKnowledge entry = scoped.get(id);
            if (entry == null) {
                entry = knowledge.full(row);
                issue(data, "CONTEXT_SCOPE_UNKNOWN", id);
            }
            if ("PENDING".equals(row.getStatus())) {
                issue(data, "KNOWLEDGE_PENDING", id);
            }
            result.add(entry);
        }
        return result;
    }

    private List<ModelRelatedGroup> related(ModelContextData data, List<String> requested, Map<String, ModelKnowledge> scoped) {
        List<String> ids = requested.isEmpty() ? Lists.newArrayList(scoped.keySet()) : requested;
        data.getQuery().setKnowledgeIds(ids);
        Map<String, ModelRelatedGroup> groups = Maps.newLinkedHashMap();
        for (KnowledgeItem row : support.relatedComponents(data.getQuery())) {
            ModelRelatedGroup group = groups.get(row.getKnowledgeId());
            if (group == null) {
                group = ModelRelatedGroup.builder().knowledgeId(row.getKnowledgeId()).build();
                groups.put(row.getKnowledgeId(), group);
            }
            group.getItems().add(ModelRelatedItem.builder().id(row.getId()).componentId(row.getTargetId()).unconditional(row.isUnconditional())
                    .condition(row.getConditionText()).exception(row.getExceptionText()).replacesKnowledgeId(row.getReplacesKnowledgeId()).build());
        }
        return Lists.newArrayList(groups.values());
    }

    private List<ModelCodeLocation> locations(ModelContextData data) {
        List<ModelCodeLocation> result = Lists.newArrayList();
        Set<String> services = Sets.newHashSet();
        for (KnowledgeItem row : data.getInterfaces()) {
            services.add(row.getServiceCode());
        }
        for (KnowledgeItem row : data.getServices()) {
            if ("FRONTEND".equals(row.getServiceType()) || ("BACKEND".equals(row.getServiceType()) && services.contains(row.getServiceCode()))) {
                location(row, data, result);
            }
        }
        return result;
    }

    private void location(KnowledgeItem row, ModelContextData data, List<ModelCodeLocation> result) {
        if (row.getLocalPath() == null || row.getLocalPath().trim().isEmpty()) {
            issue(data, "CODE_LOCATION_MISSING", row.getId());
            return;
        }
        result.add(ModelCodeLocation.builder().id(row.getId()).serviceCode(row.getServiceCode()).serviceType(row.getServiceType())
                .localPath(row.getLocalPath()).build());
    }

    private boolean included(ModelDetailsRequest request, String value) {
        return request.getInclude() != null && request.getInclude().contains(value);
    }

    private boolean empty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private List<String> unique(List<String> values) {
        return values == null ? Lists.newArrayList() : Lists.newArrayList(Sets.newLinkedHashSet(values));
    }

    private void issue(ModelContextData data, String type, String target) {
        data.getIssues().add(ModelIssue.builder().type(type).target(target).build());
    }
}