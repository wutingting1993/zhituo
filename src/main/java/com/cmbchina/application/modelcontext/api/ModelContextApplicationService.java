package com.cmbchina.application.modelcontext.api;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelComponentResponse;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsRequest;
import com.cmbchina.application.modelcontext.api.dto.ModelDetailsResponse;
/** 模型首包及按需补充边界。 */
public interface ModelContextApplicationService {
    /** 返回首包，前端接口保持独立。 */
    ModelComponentResponse component(ModelComponentRequest request);

    /** 批量读取明确请求的详情。 */
    ModelDetailsResponse details(ModelDetailsRequest request);
}