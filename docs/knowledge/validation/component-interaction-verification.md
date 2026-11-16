# 组件交互与联动验证记录

日期：2026-11-16。范围：新增两表及组件知识详情查询，不涉及前端、不新增维护接口。

## 数据库实际执行

使用项目 application.yml 的 datasource 配置连接本机 MySQL 8.0.46，执行前查询确认目标数据库为 zhituo，knowledge_component 已存在，两张新增表不存在。未打印或写入凭据。

实际执行命令：

```sh
python3 /private/tmp/zhituo-interaction-db.py database/component-interaction.sql
```

退出码 0。随后 SHOW CREATE TABLE 核对 knowledge_component_interaction、knowledge_component_relation 的字段、中文注释和三个业务查询索引；无外键或数据库枚举。结构创建后、真实资料登记前两表行数均为 0，未插入模拟或业务数据。

完整 knowledge-schema.sql 包含 14 张表，其新增两表与增量脚本结构一致。旧表及数据不作覆盖。

## 静态、编译与架构

JDK 17.0.12，项目编译目标 Java 8。Maven 本地依赖目录按项目门禁使用 .tool-cache/m2。

实际执行 verify 和门禁，FILES 为下方逐项列出的 15 个 Java 文件：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
PATH="$JAVA_HOME/bin:$PATH"
bash scripts/java-quality.sh verify $FILES
bash scripts/backend-gate.sh $FILES
```

两条命令退出码均为 0。Checkstyle 0 violations；包含作者及日期标签的提示，但未隐藏或降级检查。门禁的格式检查、167 个源文件编译、架构检查通过；架构规则执行 1 项，0 失败、0 错误。未新增或运行业务单元/e2e 测试。

Java 检查范围：

- `src/main/java/com/cmbchina/domain/knowledge/model/ComponentInteraction.java`
- `src/main/java/com/cmbchina/domain/knowledge/model/ComponentRelation.java`
- `src/main/java/com/cmbchina/infrastructure/knowledge/dao/po/ComponentInteractionPo.java`
- `src/main/java/com/cmbchina/infrastructure/knowledge/dao/po/ComponentRelationPo.java`
- `src/main/java/com/cmbchina/application/knowledge/api/dto/ComponentInteractionResponse.java`
- `src/main/java/com/cmbchina/application/knowledge/api/dto/LinkedComponentResponse.java`
- `src/main/java/com/cmbchina/application/knowledge/api/dto/ComponentRelationResponse.java`
- `src/main/java/com/cmbchina/application/knowledge/api/dto/ComponentCollaborationResponse.java`
- `src/main/java/com/cmbchina/application/knowledge/api/dto/ComponentKnowledgeResponse.java`
- `src/main/java/com/cmbchina/domain/knowledge/support/KnowledgeDataSupport.java`
- `src/main/java/com/cmbchina/infrastructure/knowledge/dao/mapper/KnowledgeMapper.java`
- `src/main/java/com/cmbchina/infrastructure/knowledge/dao/converter/KnowledgeDataConverter.java`
- `src/main/java/com/cmbchina/infrastructure/knowledge/supportimpl/KnowledgeDataSupportImpl.java`
- `src/main/java/com/cmbchina/application/knowledge/support/KnowledgeContextAssembler.java`
- `src/main/java/com/cmbchina/application/knowledge/service/ComponentKnowledgeApplicationServiceImpl.java`

## SQL 与接口

- Mapper XML 解析成功，15 个语句 ID 无重复。
- 从最终 XML 提取 interactions 和 componentRelations 的 SQL，在真实数据库执行查询和 EXPLAIN，均成功。
- 交互按 component_id 索引读取；关联候选通过交互所属组件索引和关系目标索引分别筛选，再 UNION 关系 ID。
- 关联涉及交互、联动、两端组件及两端页面，均通过明确主键或关联键连接，系统项目过滤两端同时生效，没有笛卡尔积。空表计划外层仍可能由优化器选择 ALL，且排序出现临时表与 filesort；缺少代表性关联数据，不宣称容量或性能达标。
- 最终实现以本机临时端口 18106 启动，完成只读 HTTP 核对；不改变数据库数据。以下记录仅保留关键字段：

```json
[
  {
    "scenario": "registered component",
    "httpStatus": 200,
    "code": 200,
    "componentPresent": true,
    "interactions": [],
    "collaboration": {
      "inputs": [],
      "outputs": []
    },
    "issues": [
      "KNOWLEDGE_PENDING"
    ]
  },
  {
    "scenario": "missing component",
    "httpStatus": 200,
    "code": 200,
    "componentPresent": false,
    "interactions": [],
    "collaboration": {
      "inputs": [],
      "outputs": []
    },
    "issues": [
      "COMPONENT_NOT_FOUND"
    ]
  },
  {
    "scenario": "missing page",
    "httpStatus": 200,
    "code": 200,
    "componentPresent": false,
    "interactions": [],
    "collaboration": {
      "inputs": [],
      "outputs": []
    },
    "issues": [
      "PAGE_NOT_FOUND",
      "COMPONENT_NOT_FOUND"
    ]
  }
]
```

组件已登记时保留原知识，交互和联动为空；缺组件或页面时保留原缺口提示，新增内容依旧为稳定空结构。

## 限制

首次结构验证时没有登记真实交互和联动数据，有数据时的组装当时未实测；后续已按用户要求登记真实资料并完成双向查询，见 docs/component-interaction-registration.md。跨页面关系尚无实际资料；本轮不通过写入模拟资料替代真实记录。不修改前端，其详情内容尚未接入本次新增字段。接口字段与维护边界见 docs/component-knowledge-api.md 和 database/README.md。
