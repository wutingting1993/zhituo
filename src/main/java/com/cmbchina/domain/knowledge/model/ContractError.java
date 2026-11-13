package com.cmbchina.domain.knowledge.model;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 结构化知识数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractError {
    private String id;
    private String interfaceId;
    private String code;
    private String description;
    private String triggerCondition;
    private int httpStatus;
}
