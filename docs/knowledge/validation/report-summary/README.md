# 顶部指标知识真实录入与结构化验证

日期：2026-11-13。当前记录已更新至结构化契约版本；旧manifest.json、checks.json仅是首次15条知识录入的历史证据，不能作为本轮计数依据。

## 范围与身份

系统LQ13.03、项目zhituo、后端服务zhituo_stat均由用户提供。页面路径为本地文件解码路径/Users/natasha/Downloads/智拓报表.html，组件为实际DOM标识kpiStrip，名称顶部指标统计。流程为依据当前功能提取的“报表检索与顶部指标查看”，不代表组织级流程定义。页面数据按调用接口获取理解，不包含样式知识。

代码目录：前端/Users/natasha/Downloads（只代表HTML所在目录），后端/Users/natasha/data/code/zhituo。查询地址/zhituo_stat/api/v1/reports/summary的服务前缀用于知识定位，直接调用本地业务服务仍是/api/v1/reports/summary。

## 迁移及实际数据

已执行[结构增量SQL](../../../../database/structured-interface-context.sql)和[结构化数据SQL](../../../../database/structured-interface-context-data.sql)，并成功重放验证幂等。三张新表为knowledge_interface_field、knowledge_interface_example、knowledge_interface_error；现有表增加标题、分类、名称及媒体类型。所有新增主键由项目CUuid生成，脚本固定ID；未修改报表业务表。用户明确要求不保留本案例旧数据后，已执行remove-knowledge-compatibility.sql清理旧长文本及混合知识副本；有效知识不变，未删除报表业务数据。

当前有效知识24条（业务13、技术11），关系45条，接口规则22条，字段31条（入参6、出参25），真实示例1条、错误约定2条。详见[当前清单](structured-manifest.json)。当前旧知识未分类时为NULL，不自动按内容猜测；新案例已全部明确分类，接口返回KNOWLEDGE_TYPE_MISSING提示其他尚未分类的历史数据。该做法避免将未知旧知识默认归为技术知识。

业务定义和技术实现分别存条目、复用同一适用范围。字段meaning为业务含义，detail为技术说明；接口示例直接返回JSON对象，错误触发条件字段保持triggerCondition。旧记录已删除，不提供兼容数据。

## 实际命令与结果

- python3 /tmp/apply_structured.py：增量DDL及真实数据迁移，两份脚本退出0，重放仍保持相同有效知识、关系及字段数量。
- bash scripts/java-quality.sh verify src/main/java/com/cmbchina/adapter/knowledge src/main/java/com/cmbchina/application/knowledge src/main/java/com/cmbchina/domain/knowledge src/main/java/com/cmbchina/infrastructure/knowledge：35个Java文件，0 Checkstyle violations，退出0。
- bash scripts/backend-gate.sh src/main/java/：全项目静态检查、格式检查、Java8编译、架构检查通过，退出0。未新增业务单元测试。
- mvn -B -Dmaven.repo.local=.tool-cache/m2 spring-boot:run -Dspring-boot.run.arguments=--server.port=18081：Java8环境下启动临时服务；完成后停止。
- python3 /tmp/verify_structured.py：五项真实HTTP验证通过，见[检查结果](structured-checks.json)。
- java -cp 本次服务classpath StructuredKnowledgeValidation：真实Spring事务内插入临时坏数据并查询，随后回滚，退出0；具体结果如下。
- python3 /tmp/structured_explain.py：10条查询EXPLAIN全部成功，见[执行计划](structured-explain.txt)。当前数据规模较小，不作为生产性能证明。

## 真实HTTP场景

| 场景 | 结果 |
| --- | --- |
| [完整查询](full-response.json) | 24条知识、45条关系；6入参、25出参；22规则、1 JSON示例、2错误；关系包含真实名称与定位 |
| [无接口列表](component-only-response.json) | 组件、页面、流程知识正常返回，无接口契约 |
| [组件缺失](missing-component-response.json) | 返回COMPONENT_NOT_FOUND，已匹配接口字段和规则仍返回 |
| [错误方法](wrong-method-response.json) | GET不匹配POST，空契约并返回INTERFACE_NOT_FOUND |
| [单项接口缺失](partial-interface-response.json) | 已匹配接口契约完整保留，另一接口未命中单独提示 |

已核对字段稳定ID、完整层级、meaning/detail、required/nullable、分类、triggerCondition、JSON对象而非转义文本。无效分类不自动补造，待确认业务差异不静默解决。

## 损坏知识隔离及回滚

临时验证器位于/tmp，不属于产品接口或仓库测试套件。使用Spring TransactionTemplate在同一事务内增加两个循环字段、一个孤儿字段、一个带多余尾部内容的非法JSON示例，并将一条知识分类暂改NULL。查询返回3个FIELD_STRUCTURE_INVALID、EXAMPLE_JSON_INVALID及KNOWLEDGE_TYPE_MISSING；原6入参和1合法示例仍保留。随后强制回滚并重新查询，确认不存在临时数据或分类修改残留。没有把模拟数据回退为正常业务响应。

## 审阅与实现取舍

独立审阅提出历史知识分类缺失未提示，已补充KNOWLEDGE_TYPE_MISSING并通过上述事务验证。JSON解析启用尾部令牌校验，使用Spring托管ObjectMapper，不改变全局配置。

迁移期知识分类列允许历史NULL而不设置默认技术类型，避免伪造历史分类；本案例数据全部明确分类。字段父子错误和坏示例按项隔离；其余合法知识返回。旧接口契约和混合条目已在用户明确授权后定向删除；业务代码不再包含旧类型过滤或旧格式兼容分支，只查询有效状态。

## 原统计接口验证边界

[业务请求](summary-business-request.json)、[业务响应](summary-business-response.json)保留此前实际调用：日期2026-11-11，规划8、技能3、客户方案15、应执行任务13、已执行11、待执行2、失败1、推荐客户8、覆盖支行4。仅证明接口可调用及任务分解一致，未重新独立计算全部统计，也未做并发或生产性能验证。

## 最新返回契约及删除核对

顶层仅返回businessKnowledge、technicalKnowledge、applicabilities、interfaceContexts、codeLocations、issues，无knowledgeEntries。业务13条、技术11条，关系45条；知识项只有id/title/type/kind/content/source/status；代码位置独立DTO；关系只通过knowledge及target定位，不重复knowledgeId、targetType、targetId。

本轮实际检查命令：相同四个knowledge目录执行java-quality.sh verify，共35文件通过；bash scripts/backend-gate.sh src/main/java/通过；python3 /tmp/verify_structured.py五项真实HTTP验证通过且断言无兼容字段；python3 /tmp/check_current_knowledge.py确认旧类型记录为0，业务13、技术11、关系45。

删除曾被自动审批拒绝，因为当时“不要兼容”没有明确授权永久删除；用户随后明确“不保留数据”，重新审核通过后执行定向清理。没有绕过审批或删除其他系统项目、报表业务数据。

## 页面与组件描述补充

已执行[页面组件描述SQL](../../../../database/page-component-description.sql)，只增加knowledge_page.description、knowledge_component.description并更新指定案例。查询顶层新增page、component独立对象，关联target也包含description。没有新增描述编辑接口。

本次37个knowledge Java文件verify退出0，全项目backend-gate退出0；python3 /tmp/verify_structured.py六项真实HTTP验证通过，新增断言覆盖页面/组件名称及描述、关系描述、组件缺失仍返回页面、页面缺失但接口正常。无接口列表时仍返回页面和组件基本信息。原有24条分类知识、45条关系、结构化契约数量不变。本次未增加单元或e2e测试。

本次新增页面查询后，python3 /tmp/structured_explain.py已验证11条SQL，全部成功；当前数据规模不作为生产性能证明。

## 适用范围合并到知识scopes

用户已批准：删除顶层applicabilities，业务及技术知识每条增加scopes数组。每个scope保存关系id、target、unconditional、condition、exception、replacesKnowledge，不再重复父知识引用。接口rules也使用scopes，且只包含匹配当前接口的适用项。数据库分表及数据保持不变。

本次相同四个knowledge Java目录共37文件执行java-quality.sh verify退出0；bash scripts/backend-gate.sh src/main/java/退出0；python3 /tmp/verify_structured.py六项真实HTTP验证通过。13条业务知识、11条技术知识、45条scope保持完整，接口规则仍为22条。独立代码审阅未发现实质问题。当前完整响应及案例文档已刷新，早期记录中的顶层applicabilities描述不再适用。

## 最新契约：精简顶层关系且不返回source

用户最终批准恢复顶层applicabilities，移除每条知识和接口规则的scopes。关系仅包含id、knowledgeId、targetType、targetId、unconditional、condition、exception、replacesKnowledgeId，不重复对象信息或知识正文。所有响应层级均不返回source；数据库来源数据仍保留。当前页面和组件描述继续由page、component返回。本节覆盖前述scopes安排。

本次同样四个knowledge目录共33个Java文件执行java-quality.sh verify通过；bash scripts/backend-gate.sh src/main/java/通过；python3 /tmp/verify_structured.py六项真实HTTP通过，同时验证24条正文、45个唯一关系ID、8条有条件关系及整个响应递归无source。数据库数据及结构未改动。独立审阅和最新完整响应见本轮记录。

## 最新契约：applicabilities按knowledgeId分组

用户批准applicabilities数组按knowledgeId聚合，每组仅knowledgeId和items。items保留id、targetType、targetId、unconditional、condition、exception、replacesKnowledgeId，不重复knowledgeId。没有按对象合并关系，不丢失各自条件、例外和替代信息；接口规则仍按组内当前接口关系筛选。数据库不变，仍不返回source。

本次34个knowledge Java文件verify通过，全项目backend-gate通过；python3 /tmp/verify_structured.py六项真实HTTP验证通过。结果24个知识组、45个唯一关系ID、8条有条件关系，13条业务及11条技术知识、22条接口规则不变；部分缺失场景中分组无重复、无空items。独立审阅未发现实质问题。

## 最新语义：applicabilities仅返回其他组件

按当前返回知识ID反向查询同系统项目的显式COMPONENT关联，通过组件数据库主键排除当前对象；不返回PAGE/FLOW/INTERFACE关系，不推断继承关系产生的全部组件。当前上下文的条件、例外、替代信息保存到每条知识conditions，按完整四元组去重；接口规则在裁剪返回关系前构建，仍为22条。

本例当前没有登记其他组件，因此完整查询applicabilities=[]，知识仍为业务13、技术11，所有知识conditions已保留，4条知识具有非空适用条件。六项真实HTTP验证通过。35个knowledge Java文件verify、全量backend-gate通过。

事务正例：创建另一页面的同名kpiStrip组件，显式关联一条本次业务知识，能够返回1组1项，只包含另一组件，正确排除当前主键；随后回滚并验证无临时数据残留。坏字段、坏JSON及缺少分类隔离也通过。临时验证器直接SQL写入后清理MyBatis会话缓存，避免同事务先前查询缓存干扰验证。未修改产品写逻辑或增加编辑接口。

新增反查SQL含知识关系、知识、组件、页面4表：通过知识ID及关系索引约束范围，e/p双方校验系统项目，组件按主键关联。已执行python3 /tmp/explain_related.py，EXPLAIN通过，见related-explain.txt；没有代表规模数据，不宣称生产性能达标。

## 最新字段命名及请求位置分组

relatedComponents替代applicabilities，组内使用componentId表示其他组件数据库主键，移除targetType/targetId的对外输出。仍按knowledgeId分组，其他条件和替代信息不变。当前案例没有其他共享组件，所以relatedComponents=[]。

interfaceContexts.inputs改为位置分组数组，每组location、fields；当前BODY组6字段。outputs不变。36个knowledge Java文件verify及全量backend-gate通过，六项真实HTTP验证通过。事务内临时添加QUERY/PATH/HEADER字段，验证四个位置各自成组、BODY原6字段及嵌套结构保留；同时验证共享同名组件新字段序列化无targetType/targetId，全部临时记录回滚且无残留。独立审阅未发现实质问题。数据库无永久变更。

## 最新序列化：null属性不输出

仅组件上下文接口成功及错误JSON递归省略null对象属性，保留false、0、空字符串和空集合，数组位置不改变。使用注入ObjectMapper转换响应副本，在适配器层限定路径，不修改数据库或全局ObjectMapper设置。原始summary接口响应不受影响。

新增ComponentContextResponseAdvice.java执行单文件java-quality.sh verify通过，全量backend-gate通过。python3 /tmp/verify_null_context.py对照六种历史真实响应，只删除null后与新结果完全一致；本接口校验错误的data省略、false保留，报表接口同类错误data仍为null。已刷新完整案例和原始响应。临时验证未新增仓库单元或e2e测试。

验证时发现Spring同样接受末尾斜杠路径，初版未覆盖；已补齐路径判断并重新检查，具体验证结果见null-field-checks.json。本轮无数据或DDL修改。

## 最新示例类型：请求和响应均为String

InterfaceExample.request、response改为String，返回数据库中保存的JSON原文，包括原有格式和内部null；不再返回JSON树。仍在读取时校验JSON合法性及尾随内容，非法示例按EXAMPLE_JSON_INVALID隔离。组件上下文外层null字段省略规则保持，处理不会进入字符串内部。

两个修改Java文件执行java-quality.sh verify通过，全项目backend-gate通过。python3 /tmp/verify_example_string.py六项真实HTTP场景通过：示例均为字符串，JSON解析后与原始业务调用数据一致，其他字段与上一版真实结果一致。未修改数据库、DDL或数据导出文件。

## 最新实现：本接口专用ObjectMapper

用户明确选择专用Mapper方案后，ComponentContextResponseAdvice改为注入Spring Boot的Jackson2ObjectMapperBuilder，在PostConstruct中创建私有contextMapper并设置NON_NULL。没有注册ObjectMapper Bean，避免影响Boot默认Mapper自动配置；不改全局实例。删除JsonNode树转换及递归删除代码，以RawValue输出专用Mapper生成的JSON，避免双重编码。示例已是String，内部null文本原样保留。

单文件java-quality.sh verify及全量backend-gate通过。python3 /tmp/verify_null_context.py九项真实检查通过，结果与上一版完全一致，报表错误data=null仍保留；python3 /tmp/verify_example_string.py六项String示例检查通过。独立审阅未发现实质问题。无数据库、DDL或数据文件修改。
