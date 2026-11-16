# 独立模型上下文实际验证

日期：2026-11-13。本次仅新增模型读取接口，无DDL、永久数据变更、模型调用或前端契约变更；不代表已部署。

## 真实对象

系统LQ13.03、项目zhituo、页面/Users/natasha/Downloads/智拓报表.html、组件kpiStrip、服务zhituo_stat。数据来自当前本机数据库，非生产数据证明。

## 实际命令和结果

- bash scripts/java-quality.sh verify src/main/java/com/cmbchina/adapter/modelcontext src/main/java/com/cmbchina/application/modelcontext src/main/java/com/cmbchina/adapter/knowledge/web/ComponentContextResponseAdvice.java src/main/java/com/cmbchina/domain/knowledge/support/KnowledgeDataSupport.java src/main/java/com/cmbchina/infrastructure/knowledge/dao/mapper/KnowledgeMapper.java src/main/java/com/cmbchina/infrastructure/knowledge/supportimpl/KnowledgeDataSupportImpl.java：33文件，0 Checkstyle violations，退出0。
- bash scripts/backend-gate.sh src/main/java/：静态、格式、Java8编译、架构检查通过，退出0。
- mvn -B -Dmaven.repo.local=.tool-cache/m2 spring-boot:run -Dspring-boot.run.arguments=--server.port=18081：项目JDK8环境下启动临时服务，结束后停止。
- python3 /tmp/verify_model_api.py：真实HTTP验证通过，结果见[checks.json](checks.json)。
- java -cp 本次服务classpath ModelContextTransactionValidation：真实Spring事务内验证审阅发现的边界，完成后强制回滚；未增加仓库单元/e2e测试。
- python3 /tmp/explain_model_queries.py：新增两条按ID查询EXPLAIN成功，见[id-query-explain.txt](id-query-explain.txt)；不代表生产性能达标。

## 首包及详情验证

[首包请求](component-request.json)、[首包完整响应](component-response.json)、[详情请求](details-request.json)、[详情完整响应](details-response.json)。

首包业务全文13条、技术目录11条、接口公共规则22项。业务/技术必要正文共24条，按知识ID只定义一次，所有引用可在首包找到正文；逐值核对正文、状态、条件与原前端数据一致。6个请求字段和25个响应字段的businessRule/technicalRule逐值等于原字段meaning/detail，即数据库business_meaning/technical_description；不摘要，不从知识正文推断字段规则。首包不返回类型、样例、source或代码目录。

前端POST /api/v1/knowledge/component-context响应与原完整JSON逐值一致。两个新路径复用上下文专用NON_NULL输出，未改变前端行为。

详情验证覆盖重复知识ID、重复接口ID的parts并集、inputs/outputs/rules/errors/examples分别只返回请求部分；样例保持String，rules只提供ID；未知及跨系统/跨项目真实ID均不泄露数据，合法部分仍返回。明确请求的代码目录与其他组件才返回。接口专有技术知识仅按knowledgeId获取但未指定对应接口时，返回正文并提示CONTEXT_SCOPE_UNKNOWN，不伪造无条件适用。

无接口、组件缺失、单项接口缺失首包可部分返回；空详情、非法parts/include及空parts返回10000。末尾斜杠与标准路径等价。没有隐式模型调用、会话已读缓存或token截断。

## 审阅修复及事务验证

审阅发现并修复：

1. 不同实际地址命中同一模板接口时原本会重复字段树；匹配成功后按接口数据库ID去重，未命中按规范化地址去重。
2. 单方向详情原本会校验未请求方向的坏字段；建立校验集合前按parts方向过滤。跨方向父字段仍报无效，而非接受错误引用。

事务临时创建模板接口及共享知识关系，请求两个实际模板地址再加原summary接口：仅返回两个登记接口，共享技术规则全文一次、另一个位置只引用。临时加入坏出参，inputs-only不出现坏出参提示，outputs-only明确提示；临时加入引用出参父节点的入参仍提示FIELD_STRUCTURE_INVALID。所有临时数据回滚后模型首包与原基线完全一致。结果TEMPLATE_DEDUP_AND_SHARED_RULES、REQUESTED_DIRECTION_AND_CROSS_PARENT、ROLLBACK_AND_BASELINE均passed。

## 体积与未验证范围

同一真实数据，按紧凑UTF-8 JSON的data计算：前端29,708字节，模型首包18,804字节，减少36.7%。不是实际模型token计数，也没有把格式化缩进作为节省来源。未接入具体模型，未验证真实需求澄清质量及多轮总token；接入层需避免重复发送首包和已读部分。知识两列内容不完整时仍如实返回，本任务未替用户补写业务规则。
