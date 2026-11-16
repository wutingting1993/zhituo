package com.cmbchina.domain.knowledge.model;

import lombok.Getter;

/** 知识类别及中文描述。 */
@Getter
public enum KnowledgeKind {
    BUSINESS_RULE("业务规则"),
    METRIC_RULE("指标口径"),
    TECHNICAL_RULE("技术规则"),
    CONFLICT("口径冲突");

    private final String description;

    /** 设置知识类别的中文描述。 */
    KnowledgeKind(String description) {
        this.description = description;
    }
}
