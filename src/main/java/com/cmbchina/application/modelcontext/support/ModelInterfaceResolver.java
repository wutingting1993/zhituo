package com.cmbchina.application.modelcontext.support;
import com.cmbchina.application.modelcontext.api.dto.ModelInterfaceRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
/** 根据服务前缀解析接口，不依赖展示DTO。 */
@Component
public class ModelInterfaceResolver {
    /** 在当前系统项目候选内匹配接口。 */
    public List<KnowledgeItem> resolve(List<ModelInterfaceRequest> requested, List<KnowledgeItem> candidates, ModelContextData data) {
        List<KnowledgeItem> result = Lists.newArrayList();
        Set<String> seen = Sets.newHashSet();
        if (requested == null) {
            return result;
        }
        for (ModelInterfaceRequest input : requested) {
            KnowledgeItem parsed = parse(input, data);
            if (parsed == null) {
                continue;
            }
            String key = "ADDRESS:" + parsed.getServiceCode() + ":" + parsed.getMethod() + ":" + parsed.getPath();
            if (!seen.add(key)) {
                continue;
            }
            KnowledgeItem matched = match(parsed, candidates, data);
            if (matched.getId() != null && !seen.add("ID:" + matched.getId())) {
                continue;
            }
            result.add(matched);
        }
        return result;
    }

    private KnowledgeItem parse(ModelInterfaceRequest input, ModelContextData data) {
        String path = input.getAddress().split("[?#]", 2)[0];
        int separator = path.indexOf('/', 1);
        if (!path.startsWith("/") || separator < 2 || separator == path.length() - 1) {
            issue(data, "INVALID_INTERFACE_ADDRESS", input.getAddress());
            return null;
        }
        if (path.contains("//") || path.matches(".*\\s.*") || !input.getMethod().matches("[A-Za-z]+")) {
            issue(data, "INVALID_INTERFACE_ADDRESS", input.getAddress());
            return null;
        }
        return KnowledgeItem.builder().serviceCode(path.substring(1, separator)).method(input.getMethod().toUpperCase(Locale.ROOT))
                .path(path.substring(separator)).build();
    }

    private KnowledgeItem match(KnowledgeItem parsed, List<KnowledgeItem> candidates, ModelContextData data) {
        if (data.getServices().stream().filter(s -> "BACKEND".equals(s.getServiceType()))
                .noneMatch(s -> parsed.getServiceCode().equals(s.getServiceCode()))) {
            issue(data, "SERVICE_NOT_FOUND", parsed.getServiceCode());
            return parsed;
        }
        List<KnowledgeItem> exact = Lists.newArrayList();
        List<KnowledgeItem> templates = Lists.newArrayList();
        for (KnowledgeItem candidate : candidates) {
            if (!parsed.getServiceCode().equals(candidate.getServiceCode()) || !parsed.getMethod().equals(candidate.getMethod())) {
                continue;
            }
            if (parsed.getPath().equals(candidate.getPath())) {
                exact.add(candidate);
            } else if (matches(candidate.getPath(), parsed.getPath())) {
                templates.add(candidate);
            }
        }
        List<KnowledgeItem> matches = exact.isEmpty() ? templates : exact;
        if (matches.size() == 1) {
            return matches.get(0);
        }
        issue(data, matches.isEmpty() ? "INTERFACE_NOT_FOUND" : "INTERFACE_AMBIGUOUS", parsed.getServiceCode() + parsed.getPath());
        return parsed;
    }

    private boolean matches(String template, String path) {
        String[] left = template.split("/", -1);
        String[] right = path.split("/", -1);
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            if (!left[i].equals(right[i]) && !(left[i].matches("\\{[^/{}]+\\}") && !right[i].isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private void issue(ModelContextData data, String type, String target) {
        data.getIssues().add(ModelIssue.builder().type(type).target(target).build());
    }
}