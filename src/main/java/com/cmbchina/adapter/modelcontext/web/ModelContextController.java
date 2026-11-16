package com.cmbchina.adapter.modelcontext.web;
import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.modelcontext.api.ModelContextApplicationService;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentResponse;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
/** 独立模型读取入口，保留原展示接口。 */
@RestController
@RequestMapping("/api/v1/model-context")
public class ModelContextController {
    @Autowired
    private ModelContextApplicationService service;
    /** 获取模型首次上下文。 */
    @PostMapping("/component")
    public Result<ModelComponentResponse> component(@Valid @RequestBody ModelComponentRequest request) {
        return Result.success(service.component(request));
    }

    /** 只读取模型要求的补充资料。 */
    @PostMapping("/details")
    public Result<ModelDetailsResponse> details(@Valid @RequestBody ModelDetailsRequest request) {
        return Result.success(service.details(request));
    }
}