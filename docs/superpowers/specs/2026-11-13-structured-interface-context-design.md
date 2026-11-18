# 结构化接口知识与可读适用关系

状态：用户2026-11-13已批准，包含业务/技术分类及字段命名修正。继承组件知识查询设计；本次只结构化存储、查询及真实数据迁移，不提供编辑保存接口。

## 目标与边界

接口知识必须直接列在interfaceContexts中，以JSON对象和数组表达字段、含义、口径及示例，供前端展示。适用关系同时提供名称与定位信息。后续需要编辑，因此所有持久化字段、知识、关系、示例均保留稳定ID；本次不新增前端或修改接口。

不改变统计业务口径，不录入样式，不描述为演示设计；页面数据按调用接口获取处理。当前待确认业务口径继续保留，不静默裁决。沿用Java8、Spring Boot2.7.2、MyBatis XML和MySQL，禁止数据库JSON类型、外键及自动附加权限等模块。用户先前已明确授权本任务相关知识建表和数据录入。

## 方案选择

- text保存完整JSON契约：改动较少，后续逐项修改需要处理整份文档合并。
- 分表保存：字段和示例独立定位，规则复用现有知识条目与适用关系，适合后续编辑。用户选择此方案。

## 业务知识与技术知识分类

知识条目新增必填knowledge_type，取值BUSINESS（业务知识）、TECHNICAL（技术知识），接口JSON返回type。该维度与kind（规则、指标等具体类别）、status（确认状态）、targetType（适用对象）独立，不能以关联接口或组件推断知识类型。

- 业务知识：功能用途、业务概念、指标业务定义、业务约束、业务流程、适用条件及业务口径差异。
- 技术知识：接口契约、字段类型及协议约定、数据表字段映射、SQL筛选与去重实现、技术错误处理和精度实现方式。
- 一条知识只归一种类型。现有混合正文按实际语义拆成独立条目；例如成功推荐客户方案的业务计量含义归BUSINESS，result_type=plan及(trace_id,custuid)的物理实现映射归TECHNICAL。两者可关联同一个组件或接口，不能复制同一正文，也不能把技术实现自动升级为业务批准。
- 业务上按客户去重、分母是什么等属于业务口径；实现中采用哪个字段、JOIN条件和SQL表达式属于技术口径。舍入精度和空值语义属于对外约定，归业务知识；HALF_UP及BigDecimal实现归技术知识。
- 确认状态单独保留；混合冲突按业务与技术分别记录，不因拆分类别改变原确认依据。拆分的新ID使用CUuid，并明确原条目与拆分来源；旧混合条目标记历史，不删除追溯信息。

knowledgeEntries、interfaceContexts.rules、applicabilities.knowledge及replacesKnowledge都返回type，前端可据此分组展示，无需增加第二份知识数组。接口规则仍以明确适用关系决定范围，不因分类扩大查询。

接口字段新增business_meaning、technical_description两列，返回meaning和detail。meaning解释业务含义；detail解释协议或数据映射。type、required、nullable、validation等结构化技术属性单独返回。两种说明缺失时为null，不自动从对方复制；接口字段的双重说明不等于将同一知识条目标为两种类别。

本次真实数据迁移须逐条拆分、分类现有15条知识，迁移后知识条目数量允许变化，以新清单及实际验证为准，不继续硬编码15条的验收断言。适用关系引用新条目，保留原对象范围和条件；接口分类及字段说明作为本次迁移的一部分，不新增编辑接口。

## 存储

增量迁移，不删除原知识或现有表：

1. knowledge_interface增name、description、request_content_type、response_content_type，描述接口基本契约。
2. knowledge_interface_field：id、interface_id、direction(REQUEST/RESPONSE)、location(BODY/QUERY/PATH/HEADER)、parent_id、field_name、field_path、value_type、item_type、required_flag、nullable_flag、business_meaning、technical_description、validation_text、sort_order。使用稳定id，以接口＋方向＋位置＋字段路径唯一定位。父字段必须同接口同方向同位置，根parent_id为空；数组项类型由item_type表达，复杂对象字段按层级返回。规则先使用自然语言，不构建校验执行器。
3. knowledge_interface_example：id、interface_id、name、description、request_body、response_body、sort_order；正文存text中的合法JSON，返回时解析为JSON对象/数组/标量，不返回转义JSON字符串。现有真实调用作为示例，保留示例业务日期及来源说明，不当作固定响应。
4. knowledge_interface_error：id、interface_id、code、http_status、description、trigger_condition、sort_order，按接口＋错误码唯一。应用内部JSON响应HTTP200约定不改变。
5. knowledge_entry增title、knowledge_type；knowledge_page、knowledge_component增name。流程已有name，不新增重复名称表。缺少名称不伪造已确认名称，返回可定位路径或标识作为displayName并保留name=null。

接口规则不另建重复正文表：读取已存在的INTERFACE知识适用关系，把知识条目以稳定知识ID、标题、类别、正文、来源、状态及适用条件放入interfaceContexts.rules。每条知识仍保持唯一权威存储。字段validation_text表示字段校验，business_meaning和technical_description分别表示业务含义与技术说明；完整统计口径列入rules，本次不新增字段规则关系表。

## 返回契约

保留POST /api/v1/knowledge/component-context请求和Result包装。

interfaceContexts每项：interfaceId、name、description、method、address(输入地址)、serviceCode、path(实际登记路径)、requestPath(去除服务后的实际请求路径)、requestType、responseType、inputs、outputs、rules、errors、examples。未匹配接口保留解析身份、空数组及原缺口，name/description等未知项为null。

字段项包含id、name、path、location、type、itemType、required、nullable、meaning、detail、validation、children；根列表按sort_order、id排序；children递归包含对象子字段。required和nullable分别描述字段存在性和值可空，响应嵌套字段只在父对象存在时适用，不将两个概念混用。

rules为结构化数组，每项包含knowledgeId、title、type、kind、content、source、status、applicabilities。规则正文是自然语言，但整套契约不是长文本。示例request、response是JSON值。错误项包含稳定id、code、httpStatus、description、triggerCondition。

applicabilities改为专用DTO：id、knowledge{id,title,type,kind,status}、target{type,id,name,displayName,pagePath,componentId,serviceCode,method,path}、unconditional、condition、exception、replacesKnowledge{id,title,type,status}。对象定位字段只在适用对象类型输出有意义的值，其余null。PAGE显示页面名称和路径；COMPONENT显示组件名称、组件ID及页面；FLOW显示流程名称；INTERFACE显示接口名称、服务、方法和路径。保留knowledgeId、targetType、targetId三个旧定位字段供已有调用方过渡；新前端无需另行查询即可展示关系。

knowledgeEntries继续返回业务及技术知识正文、标题与type；旧API_CONTRACT长文本迁移后标记EXPIRED，仅留历史追溯，不作为正式接口规则重复返回。其既有INTERFACE适用关系保留并解释为已迁移，针对这类已迁移记录不生成失效警告；其他真实失效规则仍提示。迁移标记使用明确kind=API_CONTRACT_LEGACY，不根据内容猜测。interfaceContexts.fields和examples等是接口契约的权威来源，现有业务规则条目仍为业务口径权威来源。

## 查询、缺失与错误

沿既有系统项目范围查询，字段、示例、错误按本次唯一命中的接口ID批量获取；无接口命中不加载整库。规则由本次命中接口的显式适用关系汇总，不串用组件独有规则。关系对象名称批量补充，避免逐条查询。

非法示例JSON、字段父子关系错误或环引用不能静默丢弃或返回空契约：返回明确知识质量缺口，保留其他合法结果，不伪装为无知识；不修改原数据。示例解析使用Spring托管ObjectMapper。稳定ID通过CUuid生成，迁移重放沿用已生成ID；迁移只针对本案例，不改写其他知识。接口契约缺少结构化字段时明确提示，不从旧长文本运行时推断结构。

## 真实数据与验收

为LQ13.03/zhituo/zhituo的summary录入6个请求字段、全部响应字段及嵌套mostUsedSkill字段，字段数以docs/report-api.md逐项核对。迁移原真实请求响应为JSON示例，录入10000、99999等已知错误约定；不借此重定义通用错误码。

验证：完整知识查询的interfaceContexts直接包含字段含义、类型、必填/可空、规则、JSON示例和错误；关系包含真实名称及定位；无接口列表、接口方法不匹配和组件缺失场景仍可返回合理部分结果；旧接口长文本不再充当正式契约。迁移后业务与技术知识分类可直接用于展示，混合条目已拆分，确认状态及来源不丢失。必要静态检查、编译、架构检查及获授权的真实数据HTTP验证通过；本次不自行追加单元/e2e测试。

## 实现者可决定

DTO文件名、XML语句拆分和方法命名沿当前knowledge模块；不引入新依赖或通用规则引擎。保持领域、PO、DTO边界；接口文档、迁移SQL和真实案例文档在同一任务同步。后续修改API的校验、保存事务及并发语义另行设计，不以只读查询暗示已实现编辑能力。

## 返回字段命名确认

仅简化JSON字段名，不删减信息，不要求同步改动数据库列名。字段business_meaning返回meaning，technical_description返回detail；知识分类knowledge_type返回type（BUSINESS/TECHNICAL），接口字段的type仍表示数据类型，两者位于不同对象。接口请求字段列表为inputs，响应字段列表为outputs；请求及响应媒体类型分别为requestType、responseType。错误触发条件明确保留triggerCondition，不使用when。required、nullable、validation、children等信息仍保留，不能将本轮命名简化理解为字段裁减。

## 实施记录

已迁移历史混合条目明确标为MIXED_LEGACY，与API_CONTRACT_LEGACY一样不作为当前正文或失效告警；历史记录及来源保留。知识分类列为历史未确认数据允许NULL，不设置TECHNICAL默认值，查询返回KNOWLEDGE_TYPE_MISSING；本次真实案例全部有效知识已明确分类。此取舍防止迁移替用户猜测旧知识类别。真实事务隔离验证在进程内同一Spring事务执行并回滚，HTTP场景另行验证，避免未提交测试数据对其他连接不可见。

## 实施记录

已迁移历史混合条目明确标为MIXED_LEGACY，与API_CONTRACT_LEGACY一样不作为当前正文或失效告警；历史记录及来源保留。知识分类列为历史未确认数据允许NULL，不设置TECHNICAL默认值，查询返回KNOWLEDGE_TYPE_MISSING；本次真实案例全部有效知识已明确分类。此取舍防止迁移替用户猜测旧知识类别。真实事务隔离验证在进程内同一Spring事务执行并回滚，HTTP场景另行验证，避免未提交测试数据对其他连接不可见。

## 最新用户指示：不兼容、不保留本案例旧数据、知识列表拆分

本节覆盖前文所有旧定位字段兼容及保留本案例历史数据的安排。顶层knowledgeEntries删除，改为businessKnowledge和technicalKnowledge；关系去掉重复knowledgeId、targetType、targetId，仅保留knowledge和target对象；知识与代码目录使用独立DTO。字段分类不明的条目提示并隔离，不默认归为技术知识。查询按有效状态读取，无旧类型运行时分支。用户明确“不保留数据”后，已删除本案例旧契约及混合知识副本与关系，当前有效数据与报表业务表保留。

## 页面组件描述补充

用户要求接口返回页面、组件描述。增加knowledge_page.description及knowledge_component.description可空text字段；顶层page{id,name,path,description}、component{id,componentId,name,description}，未登记对应对象为null。页面与组件独立查询，不依赖适用关系是否存在。关系target同步带description；不推断缺失描述，不新增编辑接口。

## 最新用户批准：适用关系内嵌知识

顶层applicabilities移除；businessKnowledge与technicalKnowledge每条知识增加scopes，合并本条知识适用关系。scope保留关系ID、target、unconditional、condition、exception及replacesKnowledge，去掉重复父知识引用。接口rules同步使用scopes，仅包含当前接口对应的关系。数据库继续分表，无存储或数据迁移。本节覆盖前文独立返回适用关系的安排。

## 最新用户确认：恢复精简applicabilities，删除响应source

恢复顶层applicabilities，仅id、knowledgeId、targetType、targetId、unconditional、condition、exception、replacesKnowledgeId。移除知识及接口规则中的scopes，不重复知识、对象元数据；业务与技术知识独立数组不变。source不在任何响应层级出现，数据库来源保留。数据库不变。本节覆盖前述内嵌范围安排。

## 最新分组确认

applicabilities按knowledgeId分组，每组knowledgeId、items；关系项不重复knowledgeId，保留id、targetType、targetId、unconditional、condition、exception、replacesKnowledgeId。条件仍属单条关系，不按对象合并不同条件，不提升到组上。仅涉及当前查询的适用关系，非全系统引用清单。数据库不变，source仍不返回。

## 最新用户同意：仅其他组件关联

applicabilities按知识ID反查同系统项目内其他显式COMPONENT关系，按当前组件数据库主键排除，页面/流程/接口关系不返回；同名异页组件不排除。当前条件及例外保存在知识conditions（unconditional、condition、exception、replacesKnowledgeId），四项完全一致去重；接口规则仍按原当前上下文构建。不推断继承范围或全系统代码影响，空数组不代表无潜在影响。数据库无需变更。

## 最新用户确认：relatedComponents及输入位置分组

relatedComponents替代applicabilities，组内componentId表示其他组件数据库主键，targetType/targetId不对外输出。输入按location分组为[{location,fields}]，仅非空位置组；字段树及校验不变，outputs不变。不保留旧格式别名，数据库不变。

## 最新用户要求：省略null字段

仅组件上下文JSON响应递归省略null对象属性，包括嵌套示例和异常响应；保留false、0、空字符串、空集合及数组位置。其他接口和数据库不改变；空页面、组件及错误data以缺省表达。本规则覆盖此前响应显式null的描述，不改变知识中的字段nullable语义。

## 最新用户要求：示例使用字符串

接口示例request与response使用String，返回数据库JSON原文，保留文本格式和内部null。JSON有效性及尾部令牌检查不变；外层序列化不再展开示例正文。此要求覆盖此前示例JSON对象返回约定，数据库存储无需变化。

## 最新用户选择：专用Mapper

在示例request/response已改String的前提下，按用户明确选择使用接口私有ObjectMapper，以Boot构建器创建、NON_NULL配置序列化，删除JsonNode递归处理。不注册全局ObjectMapper Bean、不修改默认实例，保持正常/错误/斜杠路径及其他接口隔离。RawValue只承载专用Mapper已生成的合法JSON，避免双重编码；不接受未经序列化的原始拼接响应。
