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
public class ModelInterfaceParts {
    @NotBlank(message = "接口ID不能为空")
    private String id;
    @javax.validation.constraints.NotEmpty(message = "接口parts不能为空")
    private java.util.List<@NotBlank @Pattern(regexp = "inputs|outputs|rules|errors|examples", message = "接口parts不支持") String> parts;

}
