-- 智拓报表顶部指标知识：仅新增本次明确范围；不覆盖已有知识或业务数据。
-- 系统LQ13.03，项目zhituo，后端服务zhituo_stat，组件MetricsStrip。
-- 页面登记值为本地文件解码路径；未来部署路径变更需显式维护映射。
-- 所有新增主键已调用项目CUuid.nextId()生成；脚本固定这些主键以便幂等重放。
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO knowledge_project (id, system_code, project_code) VALUES ('328937da8dfaed87', 'LQ13.03', 'zhituo') ON DUPLICATE KEY UPDATE id=id;
SET @project_id=(SELECT id FROM knowledge_project WHERE system_code='LQ13.03' AND project_code='zhituo');
INSERT INTO knowledge_service (id, system_code, project_code, service_code, service_type, local_path) VALUES ('328937da8dfbed87', 'LQ13.03', 'zhituo', 'zhituo_stat', 'BACKEND', '/Users/natasha/data/code/zhituo') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_service (id, system_code, project_code, service_code, service_type, local_path) VALUES ('328937da8dfaed87', 'LQ13.03', 'zhituo', 'zhituo', 'FRONTEND', '/Users/natasha/Downloads') ON DUPLICATE KEY UPDATE id=id;
SET @service_id=(SELECT id FROM knowledge_service WHERE system_code='LQ13.03' AND project_code='zhituo' AND service_code='zhituo_stat' AND service_type='BACKEND');
INSERT INTO knowledge_page (id, system_code, project_code, page_path) VALUES ('328937da8dfced87', 'LQ13.03', 'zhituo', '/Users/natasha/Downloads/智拓报表.html') ON DUPLICATE KEY UPDATE id=id;
SET @page_id=(SELECT id FROM knowledge_page WHERE system_code='LQ13.03' AND project_code='zhituo' AND page_path='/Users/natasha/Downloads/智拓报表.html');
INSERT INTO knowledge_component (id, page_id, component_id) VALUES ('328937da8dfded87', @page_id, 'MetricsStrip') ON DUPLICATE KEY UPDATE id=id;
SET @component_id=(SELECT id FROM knowledge_component WHERE page_id=@page_id AND component_id='MetricsStrip');
INSERT INTO knowledge_flow (id, system_code, project_code, name) VALUES ('328937da8dfeed87', 'LQ13.03', 'zhituo', '报表检索与顶部指标查看') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_page_flow (id, page_id, flow_id) VALUES ('328937da8dffed87', @page_id, '328937da8dfeed87') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_interface (id, service_id, method, path) VALUES ('328937da8e00ed87', @service_id, 'POST', '/api/v1/reports/summary') ON DUPLICATE KEY UPDATE id=id;
SET @interface_id=(SELECT id FROM knowledge_interface WHERE service_id=@service_id AND method='POST' AND path='/api/v1/reports/summary');

-- UI-KPI
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e01ed87', 'LQ13.03', 'zhituo', 'BUSINESS_RULE', '检索框下的指标统计组件ID为MetricsStrip，数据通过统计接口获取。统计内容包括生效中规划、涉及技能、当日或周期任务数、推荐客户数、方案被查看率和接触率，以及覆盖支行、使用人数最多技能、人均数量和机构基准差值。', 'file:///Users/natasha/Downloads/智拓报表.html；指标条section与renderKPI', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e10ed87', '328937da8e01ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- UI-REFRESH
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e02ed87', 'LQ13.03', 'zhituo', 'BUSINESS_RULE', '指标统计按当前日期区间、分行、支行、客户经理等级和技能条件调用接口获取结果。单日与多日分别对应当日和周期统计；机构基准用于比较当前范围与所选分行整体水平。', 'file:///Users/natasha/Downloads/智拓报表.html；renderKPI、filteredManagers、renderAll', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e11ed87', '328937da8e02ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- FLOW-DATE
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e03ed87', 'LQ13.03', 'zhituo', 'BUSINESS_RULE', '顶部统计按所选排程执行日期含首尾筛选；日期必填且开始日不得晚于结束日。排程代表计划安排不代表执行；实际执行日期匹配排程日期，且执行时间不早于规划创建时间。每任务每排程日期选择最新执行；常规执行统计无执行不纳入，但任务执行补充指标包含待执行任务。', 'docs/knowledge/shared/report-rules.md RULE_DATE、RULE_TASK；docs/report-api.md 通用约定', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e12ed87', '328937da8e03ed87', 'FLOW', '328937da8dfeed87', '应用于当前summary顶部统计；不自动推广到未声明遵循该规则的其他模块。', 0, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e13ed87', '328937da8e03ed87', 'INTERFACE', @interface_id, '应用于当前summary顶部统计；不自动推广到未声明遵循该规则的其他模块。', 0, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- PAGE-FILTER
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e04ed87', 'LQ13.03', 'zhituo', 'BUSINESS_RULE', '顶部统计机构来自目标客户经理，分行编号、支行编号、等级单选；技能编号多选取或。可选字符串省略、null、空字符串视为未筛选，技能数组省略、null、空数组视为全部。编号用于筛选和去重；不校验分支行归属关系。不同层级和日期范围分别去重，不得累加每日或子维度客户数替代整体统计。', 'docs/knowledge/shared/report-rules.md RULE_SCOPE；docs/report-api.md 通用约定', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e14ed87', '328937da8e04ed87', 'PAGE', @page_id, '仅适用于本页面顶部指标统计和summary接口，其他模块以各自口径为准。', 0, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e15ed87', '328937da8e04ed87', 'INTERFACE', @interface_id, '仅适用于本页面顶部指标统计和summary接口，其他模块以各自口径为准。', 0, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-PLAN
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e05ed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', '当前summary的planCount按执行统计范围中的规划编号去重，coveredSubBranchCount按分行编号与支行编号组合去重。页面业务名称为生效中规划，当前后端不额外按发布状态、有效期起止或当前生效状态过滤，也不套用规划覆盖模块的三类来源限制。', 'docs/report-api.md 顶部统计及通用约定；docs/knowledge/modules/reports.md；docs/knowledge/shared/report-rules.md RULE_DATE', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e16ed87', '328937da8e05ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e17ed87', '328937da8e05ed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-SKILL
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e06ed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', 'skillCount为当前执行统计范围内技能编号去重数；mostUsedSkill为使用人数最多技能，按客户经理工号去重，人数并列按技能编号升序取第一项，无数据为null。名称仅展示，同一技能编号有多个名称时当前SQL选择名称最小值。', 'docs/report-api.md 顶部统计响应字段和统计实现说明', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e18ed87', '328937da8e06ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e19ed87', '328937da8e06ed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-TASK
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e07ed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', 'taskCount当前表示成功推荐客户方案数，不等同应执行任务数。仅统计result_type=plan、最新成功结果标记=1、状态SUC且客户标识非空的结果，按执行链路标识trace_id与推荐客户custuid组合去重，排除客户名单。scheduledTaskCount按目标客户经理工号、规划编号、技能编号、排程日期、任务编号确定应执行任务；executedTaskCount为存在执行记录，pendingTaskCount为没有执行记录，二者之和等于应执行；failedTaskCount为已执行任务最新记录status非success，失败也属于已执行。不拆分未来未到期与到期未执行；客户方案与任务计量单位不同。', 'docs/knowledge/shared/report-rules.md RULE_TASK、RULE_RESULT；docs/report-api.md 顶部统计', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1aed87', '328937da8e07ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1bed87', '328937da8e07ed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-CUSTOMER
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e08ed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', 'recommendedCustomerCount按当前范围成功推荐结果中非空custuid整体去重，同一客户跨技能不重复。averageTaskCount=taskCount/执行客户经理人数；averageRecommendedCustomerCount=推荐客户数/执行客户经理人数，HALF_UP取整数，无人员为null。不得以各人员或技能客户数直接相加代替当前范围去重。', 'docs/knowledge/shared/report-rules.md RULE_CUSTOMER、RULE_RATE；docs/report-api.md 顶部统计', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1ced87', '328937da8e08ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1ded87', '328937da8e08ed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-VIEW
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e09ed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', '方案被查看客户数为当前范围is_read=1执行记录对应的成功推荐客户标识去重数。viewRate=方案被查看客户数/推荐客户数×100；推荐客户数为0时为null。', 'docs/knowledge/shared/report-rules.md RULE_CUSTOMER、RULE_RATE；docs/report-api.md 顶部统计', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1eed87', '328937da8e09ed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e1fed87', '328937da8e09ed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- METRIC-CONTACT
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0aed87', 'LQ13.03', 'zhituo', 'METRIC_RULE', '接触明细通过click_event_id关联点击事件；点击事件为button_click或preview_view且template_type=sub；trace_id匹配当前执行链路，点击客户标识匹配推荐客户标识且点击日期与实际执行日期相同。接触客户数按接触明细cust_uid去重，忽略NULL但保留空字符串，不额外要求接触客户标识等于推荐客户标识。contactRate以推荐客户数为分母×100，不截断到100%；当前实现不以接通、到店或线上有效互动标志作为附加条件。', 'docs/knowledge/shared/report-rules.md RULE_CUSTOMER、RULE_RATE；docs/report-api.md 通用约定', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e20ed87', '328937da8e0aed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e21ed87', '328937da8e0aed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- RULE-BASELINE
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0bed87', 'LQ13.03', 'zhituo', 'BUSINESS_RULE', '机构基准保留统计日期与分行筛选，移除支行、等级和技能筛选，按整体去重数据算查看率和接触率，不平均支行比率。viewRateDifference及contactRateDifference为当前率减机构基准率，单位百分点；任一侧分母为0时差异为null。', 'docs/knowledge/shared/report-rules.md RULE_RATE；docs/report-api.md 顶部统计', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e22ed87', '328937da8e0bed87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e23ed87', '328937da8e0bed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- RULE-PRECISION
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0ced87', 'LQ13.03', 'zhituo', 'TECHNICAL_RULE', '比率输出为百分比数值，如62.5代表62.5%；比率与百分点差值HALF_UP保留两位，比较与差值使用未舍入值，人均整数。数量无数据为0，比率分母0为null，最广技能无数据为null；JSON数值不保证保留尾随零。', 'docs/knowledge/shared/report-rules.md RULE_RATE；docs/report-api.md 通用约定与顶部统计', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e24ed87', '328937da8e0ced87', 'COMPONENT', @component_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e25ed87', '328937da8e0ced87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- API-SUMMARY
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0ded87', 'LQ13.03', 'zhituo', 'API_CONTRACT', '接口：POST /api/v1/reports/summary；服务标识zhituo_stat；知识查询地址/zhituo_stat/api/v1/reports/summary。Content-Type: application/json。

请求参数字段、类型、必填及业务含义：
“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

请求补充约定：可选字符串省略、null或空字符串表示全部；skillIds省略、null、空数组表示全部，多个技能取或。日期是排程日期，不是请求发起日期；开始结束日期均包含当日。日期缺失、格式错误或开始日晚于结束日返回code=10000。分支行不做归属校验；不匹配时按正常查询处理。

响应字段、类型、可空及业务含义：
响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `data.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.coveredSubBranchCount` | integer(int64) | 否 | 覆盖支行数，按(分行编号,支行编号)去重 |
| `data.mostUsedSkill` | object | 是 | 使用人数最多的技能；并列按技能编号升序取第一项，无数据为null |
| `data.mostUsedSkill.id` | string | 是 | 编号，按编号识别对象 |
| `data.mostUsedSkill.name` | string | 是 | 展示名称 |
| `data.mostUsedSkill.userCount` | integer(int64) | 否 | 该技能使用人数，按工号去重 |
| `data.averageTaskCount` | integer(int64) | 是 | 人均任务数，四舍五入为整数，无人员为null |
| `data.averageRecommendedCustomerCount` | integer(int64) | 是 | 人均推荐客户数，四舍五入为整数，无人员为null |
| `data.viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.benchmarkViewRate` | number | 是 | 机构方案查看基准率 |
| `data.benchmarkContactRate` | number | 是 | 机构接触基准率 |
| `data.viewRateDifference` | number | 是 | 查看率减机构基准率，单位百分点 |
| `data.contactRateDifference` | number | 是 | 接触率减机构基准率，单位百分点 |

逐项统计与技术口径：
- planCount、skillCount、coveredSubBranchCount在当前执行统计范围分别按规划编号、技能编号、(分行编号,支行编号)去重。名称生效中规划不增加当前后端未实现的发布状态、有效期筛选。
- taskCount为成功推荐客户方案数：result_type=plan、最新成功结果标记=1、状态SUC、客户标识非空，按(trace_id,custuid)去重，排除客户名单；不是应执行任务数。
- scheduledTaskCount按目标客户经理工号、规划编号、技能编号、排程日期、任务编号确定应执行任务。executedTaskCount存在对应执行，失败也属于已执行；pendingTaskCount无执行，不区分未来未到期和到期未执行；failedTaskCount按最新执行status非success判定。应执行=已执行+待执行，失败为已执行子集；客户方案与任务单位不同。
- recommendedCustomerCount为当前范围成功推荐结果中非空custuid整体去重，不能累加每日或子维度去重数。
- mostUsedSkill按使用客户经理工号去重计数，使用人数并列按技能编号升序；名称冲突当前SQL取名称最小值。无技能为null。
- averageTaskCount=成功推荐客户方案数/执行客户经理人数；averageRecommendedCustomerCount=去重推荐客户数/执行客户经理人数。HALF_UP取整数，人员为0时为null。
- viewRate=查看状态is_read=1的执行对应成功推荐客户去重数/推荐客户数×100。
- contactRate=关联到合法点击事件的接触明细cust_uid去重数/推荐客户数×100。点击为button_click或preview_view且template_type=sub；点击trace_id匹配执行、customer_id匹配推荐客户且点击与执行同日；接触通过click_event_id关联，忽略NULL但保留空字符串，不额外要求接触客户标识等于推荐客户标识，不附加接通等条件，不截断到100%。
- benchmarkViewRate和benchmarkContactRate采用相同公式，但基准保留日期、分行条件，移除支行、等级、技能条件，以整体去重结果计算，不平均支行比率；未选分行为全行范围。
- viewRateDifference=viewRate未舍入值-基准查看率未舍入值；contactRateDifference同理，单位为百分点。任一侧分母为0时差值为null。
- 比率返回百分比数值，如62.5代表62.5%；百分比和百分点差值HALF_UP保留两位，JSON不保证尾随零；数量无数据为0，分母0时比率为null。
- 所选排程日期含首尾；实际执行日期匹配排程日期且执行时间不早于规划创建时间，每任务每天取最新执行，执行时间并列按执行主键倒序选一条。MySQL完成范围筛选、去重和聚合，Java计算比率、精确差值并组装响应。

响应及错误约定：应用JSON响应HTTP状态200，以code和success判断结果；成功code=200、success=true；请求校验错误10000、非预期错误99999，失败data=null。网关容器错误不受该JSON约定保证。完整错误码沿用现有ResponseEnum。

实际请求示例（2026-11-13读取本机现有排程数据后调用，业务日期2026-11-11）：
{
  "startDate": "2026-11-11",
  "endDate": "2026-11-11"
}

实际响应示例（历史真实调用结果，不是固定返回值，非生产数据声明）：
{
  "code": 200,
  "message": "成功",
  "data": {
    "planCount": 8,
    "skillCount": 3,
    "taskCount": 15,
    "scheduledTaskCount": 13,
    "executedTaskCount": 11,
    "pendingTaskCount": 2,
    "failedTaskCount": 1,
    "recommendedCustomerCount": 8,
    "coveredSubBranchCount": 4,
    "mostUsedSkill": {
      "id": "3286c1a62df4ed87",
      "name": "同名验证技能",
      "userCount": 2
    },
    "averageTaskCount": 4,
    "averageRecommendedCustomerCount": 2,
    "viewRate": 0.0,
    "contactRate": 0.0,
    "benchmarkViewRate": 0.0,
    "benchmarkContactRate": 0.0,
    "viewRateDifference": 0.0,
    "contactRateDifference": 0.0
  },
  "success": true
}', 'docs/report-api.md 顶部统计、统一响应；当前Controller及DTO', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e26ed87', '328937da8e0ded87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- API-SQL
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0eed87', 'LQ13.03', 'zhituo', 'TECHNICAL_RULE', '当前顶部统计由MySQL显式JOIN和非递归CTE完成执行范围选择、去重和聚合，仅向Java返回汇总结果。Java计算比率、精确差值并组装响应；当前范围与机构基准独立聚合。执行时间并列时按执行主键倒序取一条。已有功能对照记录不代表所有后续改动已验证，也不代表已达到性能预算。', 'docs/report-api.md 统计实现说明；docs/report-summary-sql-validation.md', 'CONFIRMED') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e27ed87', '328937da8e0eed87', 'INTERFACE', @interface_id, NULL, 1, NULL, NULL) ON DUPLICATE KEY UPDATE id=id;

-- UI-BACKEND-DIFFERENCE
INSERT INTO knowledge_entry (id, system_code, project_code, kind, content, source, status) VALUES ('328937da8e0fed87', 'LQ13.03', 'zhituo', 'CONFLICT', '页面业务说明与当前后端契约存在待确认的口径差异：页面生效中规划说明按当前状态、不受日期影响且按规划负责人机构归集；后端按排程及执行范围、目标客户经理机构，不额外限制生效状态。页面任务数解释为下发任务，后端taskCount表示成功推荐客户方案数，另有scheduledTaskCount等任务指标。页面接触解释为有效接通、到店或线上互动，后端按接触明细关联去重，不附加有效接通条件。页面数据按接口获取，聚合与去重以接口业务契约为依据。后续需求澄清应明确这些业务口径的取舍，不自动修改现有实现。', 'file:///Users/natasha/Downloads/智拓报表.html 指标口径、页脚及aggManagers；docs/report-api.md；docs/knowledge/shared/report-rules.md', 'PENDING') ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e28ed87', '328937da8e0fed87', 'COMPONENT', @component_id, NULL, 1, '页面业务说明与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。', NULL) ON DUPLICATE KEY UPDATE id=id;
INSERT INTO knowledge_applicability (id, knowledge_id, target_type, target_id, condition_text, unconditional, exception_text, replaces_knowledge_id) VALUES ('328937da8e29ed87', '328937da8e0fed87', 'INTERFACE', @interface_id, NULL, 1, '页面业务说明与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。', NULL) ON DUPLICATE KEY UPDATE id=id;
COMMIT;
