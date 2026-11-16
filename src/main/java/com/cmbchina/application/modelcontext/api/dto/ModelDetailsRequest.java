package com.cmbchina.application.modelcontext.api.dto;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 面向模型的上下文数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDetailsRequest {
    @NotBlank(message = "系统编号不能为空")
    private String systemCode;
    @NotBlank(message = "项目号不能为空")
    private String projectCode;
    @NotBlank(message = "页面路径不能为空")
    private String pagePath;
    @NotBlank(message = "组件ID不能为空")
    private String componentId;
    private java.util.List<@NotBlank String> knowledgeIds;
    @javax.validation.Valid
    private java.util.List<@javax.validation.constraints.NotNull ModelInterfaceParts> interfaces;
    private java.util.List<@NotBlank @Pattern(regexp = "relatedComponents|codeLocations", message = "include不支持") String> include;

}
