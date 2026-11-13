# 组件知识上下文接口

## null字段序列化约定

仅POST /api/v1/knowledge/component-context及Spring接受的末尾斜杠形式，递归省略对象属性值为null的字段，包含嵌套DTO和异常响应；示例是字符串原文，文本内部的null不处理。false、0、空字符串、空数组、空对象不省略；数组中的null元素不删除，保持索引及顺序。下表标为可空/可缺省的响应属性，值为空时直接缺省，不显式返回null。入参允许null的规则不变，字段定义中的nullable属性也不变。

组件或页面缺失时省略component/page，错误响应data为空则省略data。其他接口仍沿用各自原有约定。本接口使用专用ObjectMapper的NON_NULL配置，不修改数据库、应用服务DTO或已保存JSON示例。专用Mapper由Spring Boot构建器创建，不注册为全局Mapper Bean，不改变其他接口；不再转换JsonNode递归删除字段。

## 范围与请求

POST `/api/v1/knowledge/component-context`，Content-Type `application/json`。本次提供结构化只读查询，不新增编辑保存接口；字段、规则、示例和关系保留稳定ID。

| 字段 | 类型 | 必填/可空 | 含义 |
| --- | --- | --- | --- |
| systemCode | string | 必填非空白 | 系统编号 |
| projectCode | string | 必填非空白 | 项目号 |
| pagePath | string | 必填非空白 | 登记的页面路径 |
| componentId | string | 必填非空白 | 页面内组件标识 |
| interfaces | array | 可省略或null | 省略/null/空数组只查询组件知识 |
| interfaces[].method | string | 必填非空白 | HTTP方法，匹配时大写 |
| interfaces[].address | string | 必填非空白 | /服务标识/实际接口路径，不支持完整URL |

interfaces不允许null元素。非空但格式错误的地址按单项缺口处理。地址去除查询参数及片段后解析；禁止空服务、空实际路径、重复斜杠和空白。接口按系统、项目、服务、方法和路径匹配，路径支持整段{name}模板，精确匹配优先，多模板命中不任意选取。

## 包装及顶层响应

应用JSON响应HTTP200，以code和success判断。成功code=200；请求校验10000；非预期内部错误99999；其他沿用ResponseEnum。本接口失败时data为空则省略，网关容器错误不受此约定保证。

data中的page、component对象仅在匹配时出现，以及businessKnowledge、technicalKnowledge、relatedComponents、interfaceContexts、codeLocations、issues六个非null数组。不返回knowledgeEntries。缺口不导致已找到的知识丢失。组件不存在仍查询输入接口；接口未匹配仍返回组件知识。

## page与component：页面和组件基本信息

| 对象 | 字段 | 类型及含义 |
| --- | --- | --- |
| page | id | string，页面数据库主键 |
| page | name | string/null，页面名称 |
| page | path | string，登记的页面路径 |
| page | description | string/null，页面业务功能描述 |
| component | id | string，组件数据库主键 |
| component | componentId | string，页面内组件标识 |
| component | name | string/null，组件名称 |
| component | description | string/null，组件业务功能描述 |

页面未登记时不返回page，返回PAGE_NOT_FOUND；组件未登记时不返回component，返回COMPONENT_NOT_FOUND。页面查询独立于组件存在性。描述未填写时省略description字段，不自动从知识正文生成；这两个对象即使没有关联知识也能返回，接口知识独立查询。

## businessKnowledge与technicalKnowledge：独立知识列表

businessKnowledge只包含BUSINESS业务知识，technicalKnowledge只包含TECHNICAL技术知识。知识项包含id、title、type、kind、content、status、conditions；不返回source或scopes；不混入代码目录及关系属性。类型独立于kind具体类别和status确认状态。分类缺失或非法时返回KNOWLEDGE_TYPE_MISSING，不擅自归类，也不将该项及其关系纳入两个列表。

正文按知识ID去重，两个列表均非null。只读取CONFIRMED和PENDING有效知识，确认状态保留，来源仅留在数据库不返回，不按代码、时间或层级裁决业务冲突。本案例迁移前旧契约及混合知识已按用户明确指示删除，不提供旧数据或旧格式兼容分支。

## interfaceContexts：接口契约

| 字段 | 类型 | 可缺省 | 含义 |
| --- | --- | --- | --- |
| interfaceId | string | 是 | 命中的接口稳定ID |
| name | string | 是 | 接口名称 |
| description | string | 是 | 接口用途 |
| method | string | 否 | HTTP方法 |
| address | string | 否 | 原输入地址 |
| serviceCode | string | 否 | 服务标识 |
| path | string | 否 | 命中时为登记路径/模板，未命中为解析路径 |
| requestPath | string | 是 | 命中时的实际请求路径，不含服务前缀 |
| requestType | string | 是 | 请求媒体类型，如application/json |
| responseType | string | 是 | 响应媒体类型 |
| inputs | array | 否 | 按location分组的请求字段，每组location、fields；只返回有字段的组 |
| outputs | array | 否 | 响应根字段列表 |
| rules | array | 否 | 接口关联的业务/技术规则 |
| errors | array | 否 | 已登记错误约定 |
| examples | array | 否 | 已登记JSON示例 |

未命中接口仍保留身份，契约数组为空，未知属性为null；interfaceId对应issues可区分未登记、歧义等情况。此结构替代旧API_CONTRACT长文本契约，不从Markdown运行时猜测字段。

### inputs位置分组

inputs每组包含location:string（BODY/QUERY/PATH/HEADER）和fields:array。fields为该位置的根字段树，只返回非空组。组按对应字段首次出现顺序返回，字段按sort_order、id排序；父子字段必须同位置，子字段保留在父字段children内，不单独进入分组。未匹配接口或无请求字段时inputs=[]。outputs保持原字段树结构。

### inputs[].fields和outputs字段项

| 字段 | 类型 | 可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 否 | 字段稳定ID |
| name | string | 否 | 字段名 |
| path | string | 否 | 完整字段路径，例如data.mostUsedSkill.id |
| location | string | 否 | BODY/QUERY/PATH/HEADER |
| type | string | 否 | string/integer/number/boolean/object/array<string>等数据类型 |
| itemType | string | 是 | 数组元素类型 |
| required | boolean | 否 | 是否要求字段存在，嵌套字段仅在父对象存在时适用 |
| nullable | boolean | 否 | 是否允许值为null，与required独立 |
| meaning | string | 是 | 业务含义 |
| detail | string | 是 | 技术说明 |
| validation | string | 是 | 字段校验约定，自然语言，不在查询时执行 |
| children | array | 否 | 对象子字段，叶字段为空数组 |

字段按sort_order、id稳定排序。孤儿、循环、父子方向或位置不一致、父字段不是对象等作为FIELD_STRUCTURE_INVALID，不影响其他合法字段。

### rules规则项

knowledgeId、title、type、kind、content、status与唯一知识条目对应。只返回通过顶层relatedComponents匹配本接口的规则，不重复返回scopes、relatedComponents或source。条件和例外统一通过顶层关系读取。type明确区分业务及技术。规则引用现有知识，不另存重复正文。业务公式与物理字段实现分别分类；缺少条件时提出澄清。

### errors错误项

id:string、code:string、httpStatus:integer、description:string、triggerCondition:string均非null。triggerCondition保留此名称。每接口错误码唯一；只返回已登记的约定，不代表枚举所有可能的网关或容器异常。

### examples示例项

id:string、name:string、description:string、request:string、response:string。request和response为数据库保存的JSON原文字符串，格式和内部null均保留；外层JSON会正常转义引号。前端可直接作为代码文本展示，需要结构化查看时自行JSON解析。示例不是固定返回值；日期及来源在description说明。数据库用text保存合法JSON，不使用JSON列。示例内容损坏返回EXAMPLE_JSON_INVALID并隔离该示例，不伪装为空契约。

## 知识项.conditions：保留当前上下文的判断依据

conditions为非null数组，每项包含unconditional:boolean、condition:string/null、exception:string/null、replacesKnowledgeId:string/null。内容来自本次检索到的当前组件、页面、流程及接口关系。四个属性完全相同才合并；不将多条条件拼成单一规则，不擅自判断适用性。没有对象元数据或知识正文副本，不返回source。

## relatedComponents：共享本次知识的其他组件

仅反向查询本次返回知识的同系统、同项目显式COMPONENT关联；通过组件数据库主键排除当前组件。每个元素为一个知识组，仅包含knowledgeId:string和items:array。knowledgeId对应businessKnowledge或technicalKnowledge中的id，在整个数组中唯一；items为非null、非空数组。每条关系只属于自己的知识组，不再逐项重复knowledgeId。

| items字段 | 类型 | 可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 否 | 关系稳定ID |
| componentId | string | 否 | 其他组件数据库主键；不是页面DOM标识，勿与顶层component.componentId混淆 |
| unconditional | boolean | 否 | 是否明确无条件适用 |
| condition | string | 是 | 本条关系的适用条件 |
| exception | string | 是 | 本条关系的例外或已登记冲突 |
| replacesKnowledgeId | string | 是 | 本条关系明确替代的知识编号 |

此处条件、例外和替代信息属于其他组件的关系，不是当前组件条件；当前判断依据见知识conditions。条件不能提升为整组属性；同一对象不同条件的关系保留各自id，不合并。分组和组内项按查询中的知识ID、关系ID稳定顺序返回。无匹配知识或没有其他显式关联组件时relatedComponents为空数组。

前端按knowledgeId关联正文，再展示其他组件的items。页面、流程及接口关系不在此处返回；当前组件不会重复出现，同名组件在另一页面拥有不同数据库主键，仍可返回。只枚举显式COMPONENT关系，不推断页面/流程规则继承出的全部组件，不把空结果视为无代码影响。当前组件未匹配时不猜测排除对象，可以返回接口知识关联的已登记组件。

数据库仍分表，本轮无结构或数据迁移。接口规则先按当前上下文构建，再查询其他组件，不受返回关系裁剪影响。

## codeLocations及issues

codeLocations使用独立DTO，仅包含id、localPath、serviceCode、systemCode、projectCode，表达前后端工作区。未配置目录返回CODE_LOCATION_MISSING，不返回虚构路径。

issues项为type:string、target:string。既有类型PROJECT_NOT_FOUND、PAGE_NOT_FOUND、COMPONENT_NOT_FOUND、NO_SCOPE_KNOWLEDGE、SERVICE_NOT_FOUND、CODE_LOCATION_MISSING、INVALID_INTERFACE_ADDRESS、INTERFACE_NOT_FOUND、INTERFACE_AMBIGUOUS、EXPIRED_KNOWLEDGE、CONDITION_UNSPECIFIED、KNOWLEDGE_PENDING保留。新增FIELD_STRUCTURE_INVALID、EXAMPLE_JSON_INVALID、INTERFACE_CONTRACT_MISSING、KNOWLEDGE_TYPE_MISSING。相同知识的多条关系可能产生重复待确认提示，正文仍唯一。

## SQL、真实案例与执行顺序

全新环境依次执行基础[建表](../database/component-knowledge.sql)、[案例录入](../database/report-summary-knowledge-data.sql)、[结构增量迁移](../database/structured-interface-context.sql)、[结构化数据迁移](../database/structured-interface-context-data.sql)。本次已在当前服务数据库执行后两份脚本及[旧知识清理SQL](../database/remove-knowledge-compatibility.sql)。结构化数据迁移也已包含相同范围的清理。页面组件描述通过[描述增量SQL](../database/page-component-description.sql)创建及补录。结构和数据迁移重放复用ID，不重复插入；不改写其他项目业务数据。原接口补录及修正脚本保留历史用途，不应在结构化迁移后回放作为当前契约来源。

完整真实请求和返回见[案例文档](component-knowledge-real-example.md)，验证范围见[验证记录](knowledge/validation/report-summary/README.md)。本次不承诺已支持保存或并发编辑。
