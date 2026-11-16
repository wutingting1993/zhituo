# 面向模型的组件上下文与按需补充设计

状态：用户已于2026-11-13要求实施。本文为独立模型接口设计，不改现有前端展示接口，不实现产品代码。

## 目标及已确认边界

模型根据新需求进行澄清时，获得组件业务依据、接口输入输出及口径，并能调用接口补充知识，减少重复和展示元数据。现有POST /api/v1/knowledge/component-context继续用于前端展示，契约和行为保持。

用户已选择精简首包＋按需补充：完整提供相关业务知识，技术知识提供目录；首包同时保留接口输入输出字段、描述和口径，去除类型和样例。输出字段不再有独立description，businessRule直接取knowledge_interface_field.business_meaning，technicalRule直接取technical_description，不摘要、不人工推断、不从knowledge_entry拆分字段规则。仍保留条件、例外、确认状态及冲突。

以下输入字段也采用businessRule/technicalRule同构映射、接口路径及可选详情项是设计建议，随本文批准。页面、组件及接口自身的description继续保留。接口字段的数据类型、数组元素类型、请求响应媒体类型、字段数据库ID和排序不进入首包；必填、可空、校验、位置和父子层级仍属于必要契约。

沿用Java8、Spring Boot2.7.2、MyBatis XML、MySQL。共用现有知识数据和领域读取，新增独立模型DTO及组装，不改变前端返回以适配模型。不新增数据库、知识录入方式、代码关系绑定、代码版本、模型调用、权限、监控或开发部署编排。

## 方案比较

1. 原前端结果去重裁剪：改动小，但展示结构与模型结构耦合，仍不断携带无关细节。
2. 根据需求先检索：输入小，但首次筛选可能遗漏业务约束；本轮不将其作为唯一知识范围。
3. 独立精简入口＋按需补充：同一来源、不同组织；完整业务和必要契约先给，额外细节按需取。用户已选择。

第一阶段做确定性投影、按知识ID去重，不引入自动摘要或向量检索。

## 首包接口

建议POST /api/v1/model-context/component。沿用Result包装及项目HTTP200/业务码约定；交给模型时接入层可以只传成功的data。

入参：systemCode、projectCode、pagePath、componentId必填非空白；interfaces可省略或空，每项method、address必填。服务前缀解析及接口匹配规则与现有前端查询一致；单项缺失不影响其他合法结果。需求文本随模型对话传入，不重复要求知识服务接收。

响应data：

| 字段 | 内容 |
| --- | --- |
| systemCode/projectCode | 本次检索范围，供补充调用使用 |
| page | path、name、description；未登记省略 |
| component | id（数据库主键）、componentId（页面标识）、name、description；未登记省略 |
| businessKnowledge | id、title、content、status、conditions，相关业务正文完整返回 |
| technicalKnowledge | id、title、status，仅目录；不据标题猜测全文 |
| interfaces | id、name、description、serviceCode、method、path、inputs、outputs、rules |
| issues | 当前已知冲突、待确认及缺失项，按type＋target去重 |

知识conditions保留unconditional、condition、exception、replacesKnowledgeId，不丢弃待确认状态或把多个条件拼成一个结论。首包不返回relatedComponents、codeLocations、错误码清单、示例、source或编辑元数据。

interfaces.inputs为[{location,fields}]，只返回非空位置组；outputs保留嵌套字段树。字段保留name、path、required、nullable、validation、children和以下两项：

- businessRule = knowledge_interface_field.business_meaning原文。
- technicalRule = knowledge_interface_field.technical_description原文。

响应仍省略null属性，保留false、0、空字符串和空集合，且不改变现有前端序列化策略。位置由输入分组表达，输出保留必要位置；空children可省略，无实际值字段省略，但false、0不能丢失。字段不返回description、type、itemType、id及sortOrder。数据库原文中自带的技术类型措辞不做文本删除，不能把“去除结构化类型属性”理解为改写口径。原列只有简短释义时如实返回，不补成完整口径；空列省略，不将空值自动认定为没有业务约束。

接口级rules来自现有INTERFACE适用关系对应的knowledge_entry，字段级口径不从此推断。公共业务/技术规则保留正文、状态和条件；同一知识ID在首包正文只出现一次：业务正文先在businessKnowledge定义；接口引用其knowledgeId。尚未定义正文的技术规则在对应接口rules首次给出knowledgeId、content、status、conditions，其余出现位置只引用knowledgeId。必要规则引用必须能在本次响应中找到完整正文，不能仅有技术目录而要求再次调用才能理解首包口径。不同知识ID或字段原文不按语义相似强行合并。

## 按需详情接口

建议POST /api/v1/model-context/details，保持无状态、支持批量。

入参：systemCode、projectCode、pagePath、componentId沿首包用于确定查询及当前组件排除范围；支持以下可选项，至少请求一项，空请求按参数错误处理：

- knowledgeIds：知识ID数组，取完整知识。
- interfaces：[{id,parts}]；parts取inputs、outputs、rules、errors、examples，至少一项。
- include：relatedComponents、codeLocations，取关联组件或本地代码目录。

返回只包含本次请求类别：knowledge、interfaces、relatedComponents、codeLocations、issues；不返回未请求部分。无命中的已请求类别返回空数组并明确缺口。请求ID按系统项目范围校验，不跨范围读取，重复ID和重复parts确定性去重。

knowledge返回id、title、content、status、conditions；条件范围来自本次组件、页面、流程和明确请求接口。若请求的知识并不关联当前上下文，返回正文及确认状态，并明确标识条件范围未知，不擅自取整个项目的条件；具体标识为issues中的CONTEXT_SCOPE_UNKNOWN。

interfaces的inputs/outputs沿首包投影；rules返回关联知识ID；errors按现有错误字段返回code、httpStatus、description、triggerCondition；examples保留request/response字符串原文。详情不返回类型元数据，样例仅在明确请求examples时返回。

relatedComponents仅反查本次相关或明确请求知识的同系统项目其他显式COMPONENT引用，按当前组件数据库主键排除，沿现有分组及条件结构；不扫描全项目知识或推断全部代码影响。codeLocations仅返回当前系统项目的前端目录及本次接口涉及服务的后端目录。无接口命中时不加载所有后端目录。缺失映射明确提示。

所有示例、口径及条件按原文保留。数据状态变化依实际查询返回，不建设contextId快照、版本系统或服务端“模型已读”会话。

## 跨调用上下文管理

接入模型的一侧负责记录已交付的知识ID及接口parts，避免每轮重发首包或重复调用；这不是知识服务中的新增会话模块。知识服务允许幂等重复读取，不靠隐藏状态决定是否省略内容。保留按ID重新读取的能力，以便显式核对变化。

资料缺失、PENDING及已知业务冲突必须进入澄清。可选技术正文可延后，但适用条件、例外、必要字段约束不能为了体积静默截断。不新增任意maxTokens截断参数，也不承诺单次上下文必然完整覆盖未来新需求。

## 测算依据与限制

当前真实案例，按紧凑UTF-8 JSON data比较：前端结果29,708字节（20,296字符）；按真实字段meaning/detail直接映射、知识去重及模型投影的候选为18,804字节（12,535字符），减少36.7%。仍保留全部业务正文、接口字段及原文口径。测算候选保守保留了部分重复名称/路径等属性，最终契约实测以实现结果为准。

此前39.8%使用人工整理字段口径，不作为实施数据来源。新测算没有重写数据库口径，不保证当前两列已经保存充分的业务与技术定义。字节不是模型token，未选定接入模型和计数方式前不报实际token节省比例。主要收益来自规则正文去重及剔除展示属性，非删掉业务依据。

## 验收

1. 现有前端接口结构、内容和用途不变，独立模型入口及详情返回可实际调用。
2. 字段businessRule/technicalRule逐值等于指定数据库列，不出现模型拼写或摘要；结构化type及examples不进入首包。
3. 首包业务正文完整，接口字段/约束/层级完整，公共口径正文可解析引用，无重复正文或悬空必要规则引用；待确认差异和条件不丢失。
4. 批量详情只返回请求部分，ID跨系统项目不泄露数据，部分缺失单独提示；未知适用范围不等同无条件适用。
5. 真实案例比紧凑前端结果明显减少文本体积；不把字节比例当token比例。接入模型后以真实需求评估澄清遗漏和总token，包含后续调用，而非只比较首包。
6. 必要静态检查、编译、架构及获授权真实数据验证；不自行新增或运行单元/e2e测试。接口文档及真实出入参同任务交付。

## 留给实施者

DTO及类名、内部查询复用方式沿实际knowledge模块，尽量复用领域支持，不强制重构前端服务；数据库无需迁移。字段空列不自动补写，模型接入、token计数适配器及对话历史压缩不在此次知识接口实现范围。
