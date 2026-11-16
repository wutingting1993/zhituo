# 组件关联与交互逻辑设计

## 目标与范围

用户已确认：交互与联动分表，扩展组件详情只读查询，通过 SQL 维护，不新增编辑页面、保存接口或确认状态。用户明确授权执行数据库脚本。本轮不修改前端、不扩展代码和库表定位、不提交或推送。

## 方案取舍

采用「组件交互＋有向联动」两表方案。与在每条关系重复维护操作描述相比，一个操作只维护一次，并允许多个影响目标；没有目标的组件内部操作也能独立登记。展示时允许在不同方向重复携带交互说明，存储中不保存两份反向关系。

## 数据结构

- knowledge_component_interaction：id、component_id、action_name、trigger_condition、provided_content、response_text、sort_order。
- knowledge_component_relation：id、interaction_id、target_component_id、effect_text、condition_text、sort_order。
- 组件字段引用 knowledge_component 的数据库主键；页面内 component_id 仅作展示。来源组件由交互确定，不在联动表重复保存。
- 主键由现有 CUuid.nextId() 生成。无外键、JSON 列、数据库枚举、触发器、自动级联或新增依赖。SQL 维护时事务写入，并显式校验引用与项目范围。

## 查询行为

现有 POST /api/v1/knowledge/component-context 增加 interactions 数组及 collaboration 对象，后者始终包含 inputs、outputs 数组。

- interactions：当前组件的操作、条件、传递内容及自身响应。
- inputs：目标为当前组件的关系，带出来源操作。
- outputs：来源为当前组件的关系。
- 只查询同系统、同项目的显式关系，允许跨页面；组件自身行为放 interactions，排除来源等于目标的关联。
- 同一对组件允许多个操作、同一操作对同一目标允许不同影响条件。不按组件对去重、不递归推导传递关系。
- 空条件仅表示未登记，不判定为无条件；不执行自然语言条件、不引入确认状态。
- 交互按 sort_order、id，关联按交互 sort_order、交互 id、关系 sort_order、关系 id 稳定排序。
- 组件未找到：沿用 COMPONENT_NOT_FOUND，新增内容为空，其他知识保持原处理。无登记内容时不返回推测数据。

## 边界与维护

只新增表，不迁移或自动摄取前端脚本数据。新库结构同步至 knowledge-schema.sql；已有库执行 component-interaction.sql 一次。新增数据写入前用现有 CUuid 工具生成 ID，更新沿用原 ID；删除交互前显式处理其关系，不级联。孤立、跨项目或自关联记录不作为有效联动返回，维护者须在提交 SQL 事务前检查并纠正，不以空响应证明资料完整。

## 验证

本次 Java 文件执行 verify；门禁执行静态、格式检查、编译及架构检查。SQL 执行后核对表结构与索引，两条查询执行 EXPLAIN；空表结果不证明代表性数据下的性能。按项目约束不新增或运行单元/e2e 测试。
