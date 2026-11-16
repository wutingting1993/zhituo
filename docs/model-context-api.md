# 模型组件上下文接口

本接口独立服务于模型需求澄清，不改变现有前端POST /api/v1/knowledge/component-context。共用知识库，无数据库结构或数据变更。字段口径不摘要、不补写。

## 通用协议

JSON POST，Content-Type: application/json。应用JSON响应HTTP200，Result字段code、message、success、data；code=200且success=true表示成功，参数校验错误10000，非预期内部错误99999，其他沿用ResponseEnum。错误data为null时省略。标准路径及末尾斜杠均适用NON_NULL；保留false、0、空数组，字符串示例中的null原样保留。

成功响应建议接入层只将data传给模型，避免重复传输包装信息。接口本身不调用模型、不管理会话“已读”状态、不实现版本快照或自动token截断。

## 1. POST /api/v1/model-context/component

### 入参

| 字段 | 类型 | 必填/可空 | 含义与校验 |
| --- | --- | --- | --- |
| systemCode | string | 必填非空白 | 系统编号 |
| projectCode | string | 必填非空白 | 项目号 |
| pagePath | string | 必填非空白 | 登记页面路径 |
| componentId | string | 必填非空白 | 页面DOM/业务组件标识，非数据库主键 |
| interfaces | array | 可省略/null/空 | 当前接口列表；空时仍返回组件/页面/流程知识 |
| interfaces[].method | string | 必填非空白 | HTTP方法，转大写后匹配 |
| interfaces[].address | string | 必填非空白 | /服务标识/实际路径；仅路径，不支持完整URL |

接口数组不能含null。地址去掉查询参数和片段，第一段识别服务，剩余为实际接口路径；不允许空服务、空实际路径、重复斜杠、空白字符。支持整段{name}模板，精确匹配优先，多模板命中明确歧义，不任意选择。不同实际路径命中同一接口ID时契约只返回一次；未匹配项按规范化地址去重。未知接口、页面、组件按项返回issues，不丢失其余数据。

### data出参

| 字段 | 内容 |
| --- | --- |
| systemCode、projectCode | 本次检索范围 |
| page | path、name、description；未登记省略 |
| component | id（数据库主键）、componentId（页面标识）、name、description；未登记省略 |
| businessKnowledge | id、title、content、status、conditions，业务正文完整提供 |
| technicalKnowledge | id、title、status，技术目录，不默认携带content或conditions |
| interfaces | id、name、description、serviceCode、method、path、inputs、outputs、rules |
| issues | [{type,target}]，按type和target去重 |

目录及正文基于当前组件、页面、显式流程及传入接口的有效知识关联。status保留CONFIRMED或PENDING，不把实现事实自动当作业务批准。conditions项含unconditional:boolean、condition、exception、replacesKnowledgeId；后3项为空省略；仅四项完全一致才去重，不计算条件或裁决例外。分类不明的条目提示KNOWLEDGE_TYPE_MISSING，不擅自分类。

首包不提供source、codeLocations、relatedComponents、错误码清单或示例。页面/组件缺失时对应对象省略；空知识列表仍返回空数组。

### 字段和口径

inputs为[{location,fields}]，只返回非空位置组；outputs为字段树。每个字段：

| 字段 | 含义 |
| --- | --- |
| name | 字段名 |
| path | 完整字段路径，包括对象层级 |
| location | 仅输出字段保留位置；输入位置由分组表达 |
| required | 是否必须存在；父对象不存在时子字段不适用 |
| nullable | 业务接口字段是否允许null；与本模型响应省略null属性不同 |
| validation | 原字段校验约定，未填写省略 |
| businessRule | knowledge_interface_field.business_meaning原文，空则省略 |
| technicalRule | knowledge_interface_field.technical_description原文，空则省略 |
| children | 对象子字段，叶字段省略空children |

不提供字段id、description、type、itemType或排序属性；不解析、截断或改写两列文本，文本内部的“64位整数”等措辞仍保留。已有列只是简短释义时如实返回，不自动补成完整规则。

只校验请求方向的字段，未请求方向的坏字段不生成提示；跨方向父节点仍视为无效。字段树仅接收同接口、方向、位置的父子关系，循环、孤儿或父字段不是对象时，返回FIELD_STRUCTURE_INVALID并隔离无效字段，合法部分仍返回。根及子字段沿sort_order、id顺序。

### 公共规则去重

interfaces[].rules每项至少knowledgeId。若正文已出现在businessKnowledge或前面接口的rules，只引用ID；否则返回knowledgeId、content、status、conditions。必要引用保证在同一首包已有正文，不依赖仅有标题的技术目录；同ID正文只出现一次。不同ID不按语义相似合并，字段原文与公共规则也不擅自语义裁剪。

## 2. POST /api/v1/model-context/details

### 入参

systemCode、projectCode、pagePath、componentId与首包同样必填。以下至少请求一类：

| 字段 | 类型 | 校验及用途 |
| --- | --- | --- |
| knowledgeIds | array<string> | 取知识全文，元素非空白，重复ID去重 |
| interfaces | array<object> | 接口批量部分请求，无null元素 |
| interfaces[].id | string | 必填非空白，接口数据库主键，来自首包id |
| interfaces[].parts | array<string> | 必须非空；inputs、outputs、rules、errors、examples；未知或空元素失败 |
| include | array<string> | relatedComponents、codeLocations；其他值失败 |

相同接口ID多次出现时parts取并集。未知ID及不属于指定系统项目的ID不返回对应内容，明确提示缺口；不新增权限模块。

### data出参

只返回请求类别，不返回未请求类别。已请求但无命中时返回空数组及issues：

- knowledge：id、title、content、status、conditions。未在当前上下文找到条件时省略conditions并提示CONTEXT_SCOPE_UNKNOWN，不能解释为无条件适用。PENDING仍明确提示。
- interfaces：基本身份及所请求parts；输入输出投影同首包。rules仅返回knowledgeId，需全文时可批量请求knowledgeIds。
- errors：code、httpStatus、description、triggerCondition，只返回已登记约定，不穷举网关错误。
- examples：id、name、description、request、response；request/response是合法JSON字符串原文；非法内容或多余尾部令牌按EXAMPLE_JSON_INVALID隔离。
- relatedComponents：[{knowledgeId,items}]；items含id、componentId（数据库主键）、unconditional及有值的condition、exception、replacesKnowledgeId。只查同系统项目、指定知识的其他显式COMPONENT关系，排除当前组件主键。无knowledgeIds时取本次上下文的知识，不扫描整个项目；空结果不代表无代码影响。
- codeLocations：[{id,serviceCode,serviceType,localPath}]。四个字段均为必填非空字符串；id为knowledge_service主键，serviceType为FRONTEND前端或BACKEND后端，localPath非空白。前后端统一读取knowledge_service，同一系统内允许项目号和服务标识相同，以service_type区分。返回当前项目的前端目录；后端仅为本次明确请求接口所属BACKEND服务，前端服务不参与接口匹配。未请求接口时不枚举后端服务。目录缺失返回CODE_LOCATION_MISSING，不返回猜测地址。
- issues：始终非null数组。当前范围缺失提示可能与请求资料同时出现，合法结果仍保留。

示例请求（ID替换成真实首包ID）：

```json
{
  "systemCode": "LQ13.03",
  "projectCode": "zhituo",
  "pagePath": "/Users/natasha/Downloads/智拓报表.html",
  "componentId": "kpiStrip",
  "knowledgeIds": ["知识ID"],
  "interfaces": [
    {"id": "接口ID", "parts": ["inputs", "outputs"]}
  ]
}
```

## 3. 缺口与错误

issues为type、target；不是HTTP错误码。已知类型包括PROJECT_NOT_FOUND、PAGE_NOT_FOUND、COMPONENT_NOT_FOUND、NO_SCOPE_KNOWLEDGE、INVALID_INTERFACE_ADDRESS、SERVICE_NOT_FOUND、INTERFACE_NOT_FOUND、INTERFACE_AMBIGUOUS、KNOWLEDGE_NOT_FOUND、KNOWLEDGE_TYPE_MISSING、KNOWLEDGE_PENDING、CONDITION_UNSPECIFIED、CONTEXT_SCOPE_UNKNOWN、EXPIRED_KNOWLEDGE、FIELD_STRUCTURE_INVALID、INTERFACE_CONTRACT_MISSING、EXAMPLE_JSON_INVALID、CODE_LOCATION_MISSING。

空详情请求、缺少必填定位、空ID、非法parts/include为code=10000，不把参数错误伪装为成功。数据库或非预期异常遵循项目统一错误处理，不回退模拟数据。

## 4. 接入与验证

模型接入层自行记录已获取知识ID及接口parts，避免每轮重发首包；本服务无会话状态，每次显式查询都可读取当前数据，不承诺跨调用快照一致。

[真实首包请求](knowledge/validation/model-context/component-request.json)、[完整首包响应](knowledge/validation/model-context/component-response.json)、[批量详情请求](knowledge/validation/model-context/details-request.json)、[完整详情响应](knowledge/validation/model-context/details-response.json)、[验证记录](knowledge/validation/model-context/README.md)。紧凑JSON data大小比较不等于实际模型token计数。

[完整模型接口真实出入参文档](model-context-real-example.md)。

### 代码目录详情示例

请求（原有路径、校验和错误码不变）：
```json
{"systemCode":"LQ13.03","projectCode":"zhituo","pagePath":"/Users/natasha/Downloads/智拓报表.html","componentId":"kpiStrip","include":["codeLocations"]}
```
响应data示例：
```json
{"codeLocations":[{"id":"328937da8dfaed87","serviceCode":"zhituo","serviceType":"FRONTEND","localPath":"/Users/natasha/Downloads"}],"issues":[]}
```
issues按当前数据返回，示例不表示其他知识缺口始终为空。
