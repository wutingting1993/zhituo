package com.cmbchina.infrastructure.knowledge.dao.po;
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
public class ContractExamplePo {
    private String id;
    private String interfaceId;
    private String name;
    private String description;
    private String requestBody;
    private String responseBody;
}
