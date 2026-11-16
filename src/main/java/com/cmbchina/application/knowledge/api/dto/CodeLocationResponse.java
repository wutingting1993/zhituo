package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 本地代码目录。 */
@Getter
@Setter
@Builder
public class CodeLocationResponse {
    private String id;
    private String systemCode;
    private String projectCode;
    private String serviceCode;
    private String serviceType;
    private String localPath;
}
