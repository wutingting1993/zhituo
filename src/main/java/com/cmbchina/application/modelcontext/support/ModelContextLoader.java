package com.cmbchina.application.modelcontext.support;
import com.cmbchina.domain.knowledge.support.KnowledgeDataSupport;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.application.modelcontext.api.dto.ModelInterfaceRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/** 读取当前上下文，不改变前端查询。 */
@Component
public class ModelContextLoader {
    @Autowired
    private KnowledgeDataSupport support;
    @Autowired
    private ModelInterfaceResolver resolver;
    /** 首包按地址匹配，详情按明确ID匹配。 */
    public ModelContextData load(KnowledgeQuery query, List<ModelInterfaceRequest> requested, List<String> interfaceIds) {
        ModelContextData data = ModelContextData.builder().query(query).projects(support.projects(query)).services(support.services(query)).build();
        if (data.getProjects().isEmpty()) {
            issue(data, "PROJECT_NOT_FOUND", query.getProjectCode());
        }
        metadata(data);
        if (interfaceIds == null) {
            data.setInterfaces(resolver.resolve(requested, support.interfaces(query), data));
        } else {
            query.setInterfaceIds(interfaceIds);
            data.setInterfaces(support.interfacesByIds(query));
            missingInterfaces(interfaceIds, data);
        }
        query.setInterfaceIds(
                data.getInterfaces().stream().map(KnowledgeItem::getId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        Set<String> scopes = Sets.newLinkedHashSet();
        if (data.getComponent() != null) {
            for (KnowledgeItem scope : support.scopes(query)) {
                scopes.add(scope.getTargetType() + ":" + scope.getTargetId());
            }
        }
        for (String id : query.getInterfaceIds()) {
            scopes.add("INTERFACE:" + id);
        }
        query.setTargetKeys(Lists.newArrayList(scopes));
        query.setCurrentComponentId(data.getComponent() == null ? null : data.getComponent().getId());
        data.setKnowledge(support.knowledge(query));
        missingKnowledge(data);
        return data;
    }

    private void missingKnowledge(ModelContextData data) {
        Set<String> found = Sets.newHashSet();
        for (KnowledgeItem row : data.getKnowledge()) {
            found.add(row.getTargetType() + ":" + row.getTargetId());
        }
        for (String scope : data.getQuery().getTargetKeys()) {
            if (!found.contains(scope)) {
                issue(data, "NO_SCOPE_KNOWLEDGE", scope);
            }
        }
    }

    private void metadata(ModelContextData data) {
        KnowledgeQuery query = data.getQuery();
        List<KnowledgeItem> pages = support.pages(query);
        if (pages.isEmpty()) {
            issue(data, "PAGE_NOT_FOUND", query.getPagePath());
        } else {
            data.setPage(pages.get(0));
        }
        List<KnowledgeItem> components = support.components(query);
        if (components.isEmpty()) {
            issue(data, "COMPONENT_NOT_FOUND", query.getComponentId());
        } else {
            data.setComponent(components.get(0));
        }
    }

    private void missingInterfaces(List<String> ids, ModelContextData data) {
        Set<String> found = data.getInterfaces().stream().map(KnowledgeItem::getId).collect(Collectors.toSet());
        for (String id : ids) {
            if (!found.contains(id)) {
                issue(data, "INTERFACE_NOT_FOUND", id);
            }
        }
    }

    private void issue(ModelContextData data, String type, String target) {
        data.getIssues().add(ModelIssue.builder().type(type).target(target).build());
    }
}