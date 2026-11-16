package com.cmbchina.application.knowledge.service;
import com.cmbchina.application.knowledge.api.ComponentKnowledgeApplicationService;
import com.cmbchina.application.knowledge.api.dto.PageComponentsRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentNameResponse;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeRequest;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeResponse;
import com.cmbchina.application.knowledge.api.dto.InterfaceKnowledgeRequest;
import com.cmbchina.application.knowledge.api.dto.KnowledgeIssue;
import com.cmbchina.application.knowledge.api.dto.InterfaceContext;
import com.cmbchina.application.knowledge.api.dto.KnowledgeItemResponse;
import com.cmbchina.application.knowledge.api.dto.RelatedComponentGroup;
import com.cmbchina.application.knowledge.api.dto.CodeLocationResponse;
import com.cmbchina.application.knowledge.support.InterfaceAddressParser;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.domain.knowledge.support.KnowledgeDataSupport;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 汇总显式关联知识，不判定条件。 */
@Service
public class ComponentKnowledgeApplicationServiceImpl implements ComponentKnowledgeApplicationService {
    @Autowired
    private com.cmbchina.application.knowledge.support.KnowledgeContextAssembler assembler;
    @Autowired
    private KnowledgeDataSupport support;
    @Autowired
    private InterfaceAddressParser parser;
    /** 查询组件上下文，缺失项独立返回。 */
    public ComponentKnowledgeResponse query(ComponentKnowledgeRequest request) {
        KnowledgeQuery query = KnowledgeQuery.builder().systemCode(request.getSystemCode()).projectCode(request.getProjectCode())
                .pagePath(request.getPagePath()).componentId(request.getComponentId()).build();
        ComponentKnowledgeResponse response = ComponentKnowledgeResponse.builder().relatedComponents(Lists.newArrayList())
                .businessKnowledge(Lists.newArrayList()).technicalKnowledge(Lists.newArrayList()).interfaceContexts(Lists.newArrayList())
                .codeLocations(Lists.newArrayList()).issues(Lists.newArrayList()).build();
        loadProject(query, response);
        loadPage(query, response);
        Set<String> scopeIds = loadScopes(query, response);
        List<KnowledgeItem> services = support.services(query);
        for (KnowledgeItem service : services) {
            if ("FRONTEND".equals(service.getServiceType())) {
                location(response, service);
            }
        }
        List<KnowledgeItem> interfaces = support.interfaces(query);

        if (request.getInterfaces() != null) {
            for (InterfaceKnowledgeRequest input : request.getInterfaces()) {
                resolveInterface(input, services, interfaces, response);
            }
        }
        for (InterfaceContext context : response.getInterfaceContexts()) {
            if (context.getInterfaceId() != null) {
                scopeIds.add("INTERFACE:" + context.getInterfaceId());
            }
        }
        query.setTargetKeys(Lists.newArrayList(scopeIds));
        query.setInterfaceIds(response.getInterfaceContexts().stream().map(InterfaceContext::getInterfaceId).filter(Objects::nonNull).distinct()
                .collect(java.util.stream.Collectors.toList()));
        collectKnowledge(query, response);
        assembler.fill(query, response);
        assembler.related(query, response);
        assembler.interactions(query, response);
        return response;
    }

    /** 查询指定页面的组件名称，不加载知识正文。 */
    public List<ComponentNameResponse> pageComponents(PageComponentsRequest request) {
        KnowledgeQuery query = KnowledgeQuery.builder().systemCode(request.getSystemCode()).projectCode(request.getProjectCode())
                .pagePath(request.getPagePath()).build();
        return support.pageComponents(query).stream()
                .map(item -> ComponentNameResponse.builder().componentId(item.getComponentKey()).name(item.getName()).build())
                .collect(java.util.stream.Collectors.toList());
    }

    private void loadProject(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        List<KnowledgeItem> projects = support.projects(query);
        if (projects.isEmpty()) {
            issue(response, "PROJECT_NOT_FOUND", query.getProjectCode());
        }
    }

    private void loadPage(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        List<KnowledgeItem> pages = support.pages(query);
        if (pages.isEmpty()) {
            issue(response, "PAGE_NOT_FOUND", query.getPagePath());
            return;
        }
        KnowledgeItem page = pages.get(0);
        response.setPage(com.cmbchina.application.knowledge.api.dto.PageResponse.builder().id(page.getId()).name(page.getName()).path(page.getPath())
                .description(page.getDescription()).build());
    }

    private Set<String> loadScopes(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        Set<String> scopes = Sets.newLinkedHashSet();
        List<KnowledgeItem> components = support.components(query);
        if (components.isEmpty()) {
            issue(response, "COMPONENT_NOT_FOUND", query.getComponentId());
        } else {
            KnowledgeItem component = components.get(0);
            response.setComponent(com.cmbchina.application.knowledge.api.dto.ComponentResponse.builder().id(component.getId())
                    .componentId(query.getComponentId()).name(component.getName()).description(component.getDescription()).build());
            for (KnowledgeItem scope : support.scopes(query)) {
                scopes.add(scope.getTargetType() + ":" + scope.getTargetId());
            }
        }
        return scopes;
    }

    private void collectKnowledge(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        Map<String, KnowledgeItemResponse> entries = Maps.newLinkedHashMap();
        Map<String, RelatedComponentGroup> groups = Maps.newLinkedHashMap();
        Set<String> covered = Sets.newHashSet();
        for (KnowledgeItem item : support.knowledge(query)) {
            covered.add(item.getTargetType() + ":" + item.getTargetId());
            if (!"BUSINESS".equals(item.getKnowledgeType()) && !"TECHNICAL".equals(item.getKnowledgeType())) {
                issue(response, "KNOWLEDGE_TYPE_MISSING", item.getKnowledgeId());
                continue;
            }
            if (!entries.containsKey(item.getKnowledgeId())) {
                entries.put(item.getKnowledgeId(), entry(item));
            }
            RelatedComponentGroup group = groups.get(item.getKnowledgeId());
            if (group == null) {
                group = RelatedComponentGroup.builder().knowledgeId(item.getKnowledgeId()).build();
                groups.put(item.getKnowledgeId(), group);
            }
            group.getItems().add(assembler.relation(item));
            knowledgeIssues(item, response);
        }
        response.getRelatedComponents().addAll(groups.values());
        for (KnowledgeItemResponse entry : entries.values()) {
            if ("BUSINESS".equals(entry.getType())) {
                response.getBusinessKnowledge().add(entry);
            } else {
                response.getTechnicalKnowledge().add(entry);
            }
        }
        for (String scope : query.getTargetKeys()) {
            if (!covered.contains(scope)) {
                issue(response, "NO_SCOPE_KNOWLEDGE", scope);
            }
        }
    }

    private KnowledgeItemResponse entry(KnowledgeItem item) {
        return KnowledgeItemResponse.builder().id(item.getKnowledgeId()).title(item.getTitle()).type(item.getKnowledgeType()).kind(item.getKind())
                .content(item.getContent()).status(item.getStatus()).build();
    }

    private void knowledgeIssues(KnowledgeItem item, ComponentKnowledgeResponse response) {
        if (!"BUSINESS".equals(item.getKnowledgeType()) && !"TECHNICAL".equals(item.getKnowledgeType())) {
            issue(response, "KNOWLEDGE_TYPE_MISSING", item.getKnowledgeId());
        }
        if (!item.isUnconditional() && (item.getConditionText() == null || item.getConditionText().trim().isEmpty())) {
            issue(response, "CONDITION_UNSPECIFIED", item.getId());
        }
        if ("EXPIRED".equals(item.getReplacesStatus())) {
            issue(response, "EXPIRED_KNOWLEDGE", item.getReplacesKnowledgeId());
        }
        if ("PENDING".equals(item.getStatus())) {
            issue(response, "KNOWLEDGE_PENDING", item.getKnowledgeId());
        }

    }

    private void resolveInterface(InterfaceKnowledgeRequest input, List<KnowledgeItem> services, List<KnowledgeItem> interfaces,
            ComponentKnowledgeResponse response) {
        InterfaceContext context = parser.parse(input.getMethod(), input.getAddress());
        if (context == null) {
            issue(response, "INVALID_INTERFACE_ADDRESS", input.getAddress());
            return;
        }
        response.getInterfaceContexts().add(context);
        boolean found = false;
        for (KnowledgeItem service : services) {
            if ("BACKEND".equals(service.getServiceType()) && context.getServiceCode().equals(service.getServiceCode())) {
                found = true;
                location(response, service);
            }
        }
        if (!found) {
            issue(response, "SERVICE_NOT_FOUND", context.getServiceCode());
            return;
        }
        List<KnowledgeItem> exact = candidates(context, interfaces, false);
        List<KnowledgeItem> templates = candidates(context, interfaces, true);
        List<KnowledgeItem> matches = exact.isEmpty() ? templates : exact;
        if (matches.size() == 1) {
            KnowledgeItem matched = matches.get(0);
            context.setInterfaceId(matched.getId());
            context.setRequestPath(context.getPath());
            context.setPath(matched.getPath());
            context.setName(matched.getName());
            context.setDescription(matched.getDescription());
            context.setRequestType(matched.getRequestContentType());
            context.setResponseType(matched.getResponseContentType());
        } else {
            issue(response, matches.isEmpty() ? "INTERFACE_NOT_FOUND" : "INTERFACE_AMBIGUOUS", input.getAddress());
        }
    }

    private List<KnowledgeItem> candidates(InterfaceContext context, List<KnowledgeItem> interfaces, boolean template) {
        List<KnowledgeItem> result = Lists.newArrayList();
        for (KnowledgeItem item : interfaces) {
            if (!context.getServiceCode().equals(item.getServiceCode()) || !context.getMethod().equals(item.getMethod())) {
                continue;
            }
            boolean matches = template ? parser.matches(item.getPath(), context.getPath()) : context.getPath().equals(item.getPath());
            if (matches) {
                result.add(item);
            }
        }
        return result;
    }

    private void location(ComponentKnowledgeResponse response, KnowledgeItem item) {
        if (item.getLocalPath() == null || item.getLocalPath().trim().isEmpty()) {
            issue(response, "CODE_LOCATION_MISSING", item.getId());
            return;
        }
        for (CodeLocationResponse existing : response.getCodeLocations()) {
            if (Objects.equals(existing.getId(), item.getId()) && Objects.equals(existing.getServiceCode(), item.getServiceCode())) {
                return;
            }
        }
        response.getCodeLocations()
                .add(CodeLocationResponse.builder().id(item.getId()).systemCode(item.getSystemCode()).projectCode(item.getProjectCode())
                        .serviceCode(item.getServiceCode()).serviceType(item.getServiceType()).localPath(item.getLocalPath()).build());
    }

    private void issue(ComponentKnowledgeResponse response, String type, String target) {
        response.getIssues().add(KnowledgeIssue.builder().type(type).target(target).build());
    }

}
