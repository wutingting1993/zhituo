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

data中的page、component对象仅在匹配时出现，以及businessKnowledge、technicalKnowledge、relatedComponents、interfaceContexts、codeLocations、issues、interactions七个非null数组，以及始终存在的collaboration对象。不返回knowledgeEntries。缺口不导致已找到的知识丢失。组件不存在仍查询输入接口；接口未匹配仍返回组件知识。

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

### kind知识类别

kind在Java中使用KnowledgeKind枚举，枚举包含description中文描述字段（getDescription()）；数据库仍为varchar，接口仍返回下列英文字符串，不新增description响应字段。

| 枚举值 | description |
| --- | --- |
| BUSINESS_RULE | 业务规则 |
| METRIC_RULE | 指标口径 |
| TECHNICAL_RULE | 技术规则 |
| CONFLICT | 口径冲突 |

该定义同时适用于businessKnowledge、technicalKnowledge和interfaceContexts[].rules中的kind；原有请求参数及错误码不变。

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

conditions为非null数组，每项字段如下：

| 字段 | 类型 | 必填/可空 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填 | knowledge_applicability适用关系主键 |
| targetType | string | 必填 | COMPONENT组件、PAGE页面、FLOW流程、INTERFACE接口 |
| targetId | string | 必填 | 对应关联对象的数据库主键；COMPONENT时不是页面内componentId |
| unconditional | boolean | 必填 | 是否明确无条件适用 |
| condition | string | 可缺省 | 适用条件，未知时缺省 |
| exception | string | 可缺省 | 例外及已知冲突依据 |
| replacesKnowledgeId | string | 可缺省 | 明确替代的知识编号 |

内容仅来自本次检索命中的当前组件、所属页面、页面所属流程及输入且匹配的接口关系，不包含其他组件的反向引用。范围表示规则适用于当前组件的依据；PAGE可用于展示“页面通用”，COMPONENT可用于展示“组件专属”。接口不计算自然语言条件、不裁决例外或按范围层级自动覆盖规则；条件尚未触发的规则仍保留，供组件实现及模型理解。

正文按知识ID去重，conditions按适用关系id去重。不同关系即使条件内容完全相同也分别保留，不将多条条件拼成单一规则。不返回知识正文副本或source。请求路径、HTTP方法、参数、校验及错误码保持不变。

以下为说明性片段，同一知识同时通过页面和当前组件命中，两条无条件关系均保留（不是实际请求验证记录）：

```json
{
  "id": "K001",
  "content": "所有统计均使用页面选择的日期范围",
  "conditions": [
    {"id": "A001", "targetType": "PAGE", "targetId": "P001", "unconditional": true},
    {"id": "A002", "targetType": "COMPONENT", "targetId": "C001", "unconditional": true}
  ]
}
```

本节新增范围字段只适用于组件知识上下文接口；独立模型上下文接口保持原契约。历史真实响应及验证记录未重采集，不能作为新增字段的运行验证依据。

## relatedComponents：共享本次知识的其他组件

仅反向查询本次返回知识的同系统、同项目显式COMPONENT关联；通过组件数据库主键排除当前组件。每个元素为一个知识组，仅包含knowledgeId:string和items:array。knowledgeId对应businessKnowledge或technicalKnowledge中的id，在整个数组中唯一；items为非null、非空数组。每条关系只属于自己的知识组，不再逐项重复knowledgeId。

| items字段 | 类型 | 可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 否 | 关系稳定ID |
| componentId | string | 否 | 其他组件数据库主键；不是页面DOM标识，勿与顶层component.componentId混淆 |
| component | object | 否 | 组件展示与定位身份，结构见下文；不改变外层componentId的数据库主键语义 |
| unconditional | boolean | 否 | 是否明确无条件适用 |
| condition | string | 是 | 本条关系的适用条件 |
| exception | string | 是 | 本条关系的例外或已登记冲突 |
| replacesKnowledgeId | string | 是 | 本条关系明确替代的知识编号 |

此处条件、例外和替代信息属于其他组件的关系，不是当前组件条件；当前判断依据见知识conditions。条件不能提升为整组属性；同一对象不同条件的关系保留各自id，不合并。分组和组内项按查询中的知识ID、关系ID稳定顺序返回。无匹配知识或没有其他显式关联组件时relatedComponents为空数组。

前端按knowledgeId关联正文，再展示其他组件的items。页面、流程及接口关系不在此处返回；当前组件不会重复出现，同名组件在另一页面拥有不同数据库主键，仍可返回。只枚举显式COMPONENT关系，不推断页面/流程规则继承出的全部组件，不把空结果视为无代码影响。当前组件未匹配时不猜测排除对象，可以返回接口知识关联的已登记组件。

数据库仍分表，本轮无结构或数据迁移。接口规则先按当前上下文构建，再查询其他组件，不受返回关系裁剪影响。

## codeLocations及issues

codeLocations使用独立DTO，字段定义如下；本次请求参数、校验规则、HTTP方法、路径和错误码保持不变。

| 字段 | 类型 | 必填/可空 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填非空 | knowledge_service记录主键 |
| systemCode | string | 必填非空 | 系统编号 |
| projectCode | string | 必填非空 | 项目号，前后端允许相同 |
| serviceCode | string | 必填非空 | 服务标识，允许前后端相同 |
| serviceType | string | 必填非空 | FRONTEND前端、BACKEND后端 |
| localPath | string | 必填非空白 | 本地代码目录 |

前后端目录均读取knowledge_service，knowledge_project只保存项目身份；唯一键为(system_code, project_code, service_type, service_code)。返回当前系统项目的前端目录，后端仅返回本次输入接口所属服务；接口匹配限定BACKEND，前端服务不参与匹配。未配置目录返回CODE_LOCATION_MISSING，不返回虚构路径。

示例请求：
```json
{"systemCode":"LQ13.03","projectCode":"zhituo","pagePath":"/Users/natasha/Downloads/智拓报表.html","componentId":"kpiStrip","interfaces":[{"method":"POST","address":"/zhituo_stat/api/v1/reports/summary"}]}
```
响应data.codeLocations示例（其他响应字段沿用本文定义）：
```json
[
  {"id":"328937da8dfaed87","systemCode":"LQ13.03","projectCode":"zhituo","serviceCode":"zhituo","serviceType":"FRONTEND","localPath":"/Users/natasha/Downloads"},
  {"id":"328937da8dfbed87","systemCode":"LQ13.03","projectCode":"zhituo","serviceCode":"zhituo_stat","serviceType":"BACKEND","localPath":"/Users/natasha/data/code/zhituo"}
]
```

issues项为type:string、target:string。既有类型PROJECT_NOT_FOUND、PAGE_NOT_FOUND、COMPONENT_NOT_FOUND、NO_SCOPE_KNOWLEDGE、SERVICE_NOT_FOUND、CODE_LOCATION_MISSING、INVALID_INTERFACE_ADDRESS、INTERFACE_NOT_FOUND、INTERFACE_AMBIGUOUS、EXPIRED_KNOWLEDGE、CONDITION_UNSPECIFIED、KNOWLEDGE_PENDING保留。新增FIELD_STRUCTURE_INVALID、EXAMPLE_JSON_INVALID、INTERFACE_CONTRACT_MISSING、KNOWLEDGE_TYPE_MISSING。相同知识的多条关系可能产生重复待确认提示，正文仍唯一。

## SQL、真实案例与执行顺序

全新环境依次执行基础[建表](../database/component-knowledge.sql)、[案例录入](../database/report-summary-knowledge-data.sql)、[结构增量迁移](../database/structured-interface-context.sql)、[结构化数据迁移](../database/structured-interface-context-data.sql)。本次已在当前服务数据库执行后两份脚本及[旧知识清理SQL](../database/remove-knowledge-compatibility.sql)。结构化数据迁移也已包含相同范围的清理。页面组件描述通过[描述增量SQL](../database/page-component-description.sql)创建及补录。结构和数据迁移重放复用ID，不重复插入；不改写其他项目业务数据。原接口补录及修正脚本保留历史用途，不应在结构化迁移后回放作为当前契约来源。

完整真实请求和返回见[案例文档](component-knowledge-real-example.md)，验证范围见[验证记录](knowledge/validation/report-summary/README.md)。本次不承诺已支持保存或并发编辑。


## 组件交互与联动（2026-11-16）

仍使用 `POST /api/v1/knowledge/component-context`，请求字段、HTTP 方法、参数校验及现有错误码不变。不接收前端 `collaboration`、`behaviors` 作为保存指令，不增加保存接口。登记资料通过数据库 SQL 维护，结构见 `database/component-interaction.sql`。

### interactions：当前组件的交互逻辑

`interactions` 为非 null 数组，无登记或组件未命中时为 `[]`。只读取当前系统、项目及命中的组件；按 `sort_order, id` 排序，无关联目标的交互仍返回。

| 字段 | 类型 | 必填/可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填 | 交互稳定主键 |
| action | string | 必填 | 操作名称或触发行为 |
| triggerCondition | string | 可缺省 | 交互触发条件 |
| providedContent | string | 可缺省 | 向其他组件传递的业务内容 |
| response | string | 可缺省 | 当前组件自身的响应，不是 HTTP 响应体 |

可缺省字段无登记时按本接口约定省略；空条件不表示明确无条件，查询不执行自然语言条件。不返回确认状态，也不按确认状态筛选。

### collaboration：依赖来源与影响范围

`collaboration` 始终返回 `{ "inputs": [], "outputs": [] }`。`inputs` 是目标为当前组件的显式关系，按来源 source.id 分组；`outputs` 是来源为当前组件的显式关系，按目标 target.id 分组。分组依据为组件数据库主键，不按中文名称或页面内 componentId 合并。

两方向数组项均为 `{component, items}`：component 为本组关联组件身份（结构同下文 source/target），items 为非空联动关系数组，条目字段如下表。组内保留全部操作及条件，不合并关系。组件组按首次出现顺序返回，组内保持关系查询顺序。前端计数和前六项预览按组件组计算；关系总数为各组 items 长度之和。关系主键在两端查询中相同，数据库只保存一次。未登记或无关联时仍返回空数组。本次请求参数、路径、HTTP 方法、校验和错误码不变。

| 字段 | 类型 | 必填/可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填 | 联动关系稳定主键 |
| interactionId | string | 必填 | 来源交互稳定主键 |
| source | object | 必填 | 发起操作的组件身份 |
| target | object | 必填 | 受影响的组件身份 |
| action | string | 必填 | 来源交互的操作名称 |
| triggerCondition | string | 可缺省 | 来源交互的触发条件 |
| providedContent | string | 可缺省 | 来源交互提供的业务内容 |
| response | string | 可缺省 | 来源组件自身的响应 |
| effect | string | 必填 | 对目标组件产生的变化 |
| condition | string | 可缺省 | 仅此条联动的生效条件，与交互触发条件共同阅读 |

`source`、`target` 均包含以下字段：

| 字段 | 类型 | 必填/可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填 | 组件数据库主键 |
| componentId | string | 必填 | 所属页面内组件标识，与 id 不同 |
| name | string | 可缺省 | 登记的组件中文名称 |
| pagePath | string | 必填 | 组件所属的登记页面路径 |

两端必须属于请求的同一系统、项目；允许不同页面。来源和目标相同的行为属于组件内部交互，不在联动中展示。查询不递归推导间接影响，不根据共享知识或同用接口推测联动。不合并同一组件对的多个操作，也不合并同一操作对同一目标的不同生效条件。

各方向均按交互的 `sort_order, id`、关系的 `sort_order, id` 稳定排序。无联动返回空数组，不代表已经证明没有实现层面的影响。历史 `relatedComponents` 仍表示知识的显式适用关系，不替换为组件联动。

组件未匹配时保留现有 `COMPONENT_NOT_FOUND`，新增的交互及两个方向均为空，已匹配接口和其他知识按原逻辑继续返回。未新增业务错误码。

### 请求与响应示意

下面的 ID、名称和正文只用于说明结构，不代表已写入数据库。实际关联引用需使用现有组件主键，新交互和联动主键通过 `CUuid.nextId()` 生成。

```json
{
  "systemCode": "LQ13.03",
  "projectCode": "zhituo",
  "pagePath": "/example/report",
  "componentId": "FiltersBar",
  "interfaces": []
}
```

成功响应中的新增字段示意（其他既有字段省略）：

```json
{
  "code": 200,
  "success": true,
  "message": "成功",
  "data": {
    "interactions": [
      {
        "id": "interaction-id",
        "action": "应用筛选",
        "providedContent": "当前统计日期和机构条件",
        "response": "保存当前生效的筛选条件"
      }
    ],
    "collaboration": {
      "inputs": [],
      "outputs": [
        {
          "component": {
            "id": "summary-db-id",
            "componentId": "MetricsStrip",
            "name": "核心指标总览",
            "pagePath": "/example/report"
          },
          "items": [
            {
              "id": "relation-id",
              "interactionId": "interaction-id",
              "source": {
                "id": "filter-db-id",
                "componentId": "FiltersBar",
                "name": "筛选栏",
                "pagePath": "/example/report"
              },
              "target": {
                "id": "summary-db-id",
                "componentId": "MetricsStrip",
                "name": "核心指标总览",
                "pagePath": "/example/report"
              },
              "action": "应用筛选",
              "providedContent": "当前统计日期和机构条件",
              "response": "保存当前生效的筛选条件",
              "effect": "按新的统计范围重新查询指标"
            }
          ]
        }
      ]
    }
  }
}
```

查询上述目标组件时，返回同一关系到 `collaboration.inputs[].items`；只有该目标自己登记的操作进入 `interactions`，不把来源组件的操作复制为目标的内部交互。


### 当前真实登记与验证

2026-11-16 当前页面已按前端 meta.id 登记26个组件、39条交互、56条联动；原 kpiStrip 已改为 MetricsStrip，中文名为核心指标总览，数据库主键不变，不保留旧标识别名。56条关系的来源 outputs 和目标 inputs 已通过真实 HTTP 逐项核对；实现依据见 component-interaction-registration.md，字段示例见 knowledge/validation/component-interaction-source-example.json 和 component-interaction-target-example.json。页面登记路径未改变。


## 共享知识组件的展示身份

组件知识接口 `relatedComponents[].items[].component` 补充用于展示的组件信息，请求参数、HTTP 方法和错误码保持不变；不改变原知识适用关系或筛选范围。外层 `componentId` 仍是数据库主键，内层 `component.componentId` 才是当前前端 meta.id。

| 字段 | 类型 | 必填/可缺省 | 含义 |
| --- | --- | --- | --- |
| id | string | 必填 | knowledge_component 数据库主键，与外层 componentId 相同 |
| componentId | string | 必填 | 所属页面内的组件标识，用于同页面定位 |
| name | string | 可缺省 | 登记的中文名称，未填写时前端可展示组件标识 |
| pagePath | string | 必填 | 所属登记页面路径，用于说明上下文和判断是否可在当前页定位 |

条件、例外、替代关系仍属于每一条 items 记录，不能因组件相同而合并丢失。数据从既有组件和页面表读取，不新增表，不从 collaboration 推导知识共享。

前端在对应业务知识正文下展示「同用此口径」，按组件主键聚合名称，展开后保留各条关系的适用条件与例外。只有页面路径与当前上下文一致、且组件标识存在于当前前端注册表时提供定位入口。没有其他显式适用组件时不显示占位行。

独立模型上下文接口保持其现有精简结构。

新增展示身份的真实响应片段如下；该片段来自组件未命中、汇总接口知识已命中的只读查询，不代表所有当前组件都共享这条知识：

```json
{
  "relatedComponents": [
    {
      "knowledgeId": "328937da8e0fed87",
      "items": [
        {
          "id": "328937da8e28ed87",
          "componentId": "328937da8dfded87",
          "component": {
            "id": "328937da8dfded87",
            "componentId": "MetricsStrip",
            "name": "核心指标总览",
            "pagePath": "/Users/natasha/Downloads/智拓报表.html"
          },
          "unconditional": true,
          "exception": "页面业务说明与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。"
        }
      ]
    }
  ]
}
```

## 页面组件名称列表

`POST /api/v1/knowledge/page-components`，`Content-Type: application/json`。按系统、项目和页面查询已登记组件，不加载知识正文，不提供树层级，不修改数据。

请求字段 `systemCode`、`projectCode`、`pagePath` 均为必填、非空白 string；分别表示系统编号、项目号、登记页面路径。缺失、null 或空白按现有参数校验规则返回 code=10000；未预期异常 code=99999。应用 JSON 响应 HTTP200，使用标准 Result 信封。

成功 data 为数组，每项 `componentId:string`（必填非空，页面内组件标识）和 `name:string|null`（登记名称，允许未登记）。按组件数据库主键升序返回；页面不存在或没有组件时 data=[]，不回退其他页面、项目或系统。前端按 componentId 匹配本地树节点，名称空白、未匹配或查询失败时显示英文组件名，不再使用前端清单中文名称；树层级和实际接口调用配置保持前端维护。

请求示例：
```json
{"systemCode":"LQ13.03","projectCode":"zhituo","pagePath":"/Users/natasha/Downloads/智拓报表.html"}
```
成功响应结构示例（仅列一项）：
```json
{"code":200,"message":"成功","success":true,"data":[{"componentId":"MetricsStrip","name":"核心指标总览"}]}
```
参数错误响应结构示例：
```json
{"code":10000,"message":"系统编号不能为空","success":false,"data":null}
```
当前页面组件名称初始化沿用 `database/knowledge-data.sql` 与 `database/component-interaction-data.sql`；本次核对数据库中 26 个组件均已登记，未重复写入。组件树只改变名称来源，不增加后端层级数据。

### 组件关联分组验证

2026-11-16 经前端 5173 代理调用已更新的 18106 后端，逐项核对 26 个组件。inputs 和 outputs 展开后各保留原有 56 条关系，逐条 ID、交互、两端身份、正文及条件与分组前一致；分组身份唯一，分组顺序及组内关系顺序均保留。核心指标总览 inputs 为 1 个筛选栏组件组、3 条联动。历史 validation 中的平铺响应是改动前记录，当前接口以本节分组契约及上方示例为准。Java 修改范围 verify、编译、架构检查及前端格式、类型、lint、清单、构建检查通过；未执行单元、E2E 或浏览器实测。
