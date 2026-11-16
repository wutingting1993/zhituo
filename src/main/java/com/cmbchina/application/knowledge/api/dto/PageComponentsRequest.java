package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.validation.constraints.NotBlank;
/** 页面组件名称查询范围。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageComponentsRequest {
    @NotBlank(message = "系统编号不能为空")
    private String systemCode;
    @NotBlank(message = "项目号不能为空")
    private String projectCode;
    @NotBlank(message = "页面路径不能为空")
    private String pagePath;
}
