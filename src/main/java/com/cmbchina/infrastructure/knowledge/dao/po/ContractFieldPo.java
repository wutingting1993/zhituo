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
public class ContractFieldPo {
    private String id;
    private String interfaceId;
    private String direction;
    private String location;
    private String parentId;
    private String fieldName;
    private String fieldPath;
    private String valueType;
    private String itemType;
    private String businessMeaning;
    private String technicalDescription;
    private String validationText;
    private boolean requiredFlag;
    private boolean nullableFlag;
}
