# 数据库初始化

init.sql 面向 MySQL 8.0，在本机 zhituo 库创建需求文档中的 11 张表。不删除或覆盖已有表，不插入模拟或业务数据；已有表时 CREATE TABLE 会报错，应先核对结构，不使用 --force。

来源：/Users/natasha/data/code/规划统计需求口径.md。

适配：补齐字段中文注释；customer_info、bbk_ids 由 JSON 改为 TEXT（保存 JSON 文本，数据库不自动校验 JSON）；删除导出时的历史 AUTO_INCREMENT 起点。保留原有自增主键、字段拼写、默认值、排序规则及索引，未引入外键。

统计口径待澄清（本次未实现统计）：

- 结果索引及接触明细的 skill_id 被描述为关联场景明细 id，但场景表还存在 scene_id（技能ID），且 id 为 bigint，需确认实际关联键。
- 点击表不存在 tenant_id，存在 user_id；接触表同样为 user_id。
- 口径出现 button_typ，实际字段为 button_type；口径值 plan 与字段注释 phone 不一致。
- 口径 preview_view 与 event_type 注释 main_preview_view/sub_preview_view 不一致。
- swe_plan_sence_cron 和 sence_id 的原有拼写保留。

当前表结构沿用来源系统自增主键；后续新增业务写入的 ID 生成需另行核对项目 CUuid 约定，不能直接将字符串 CUuid 写入 bigint。

## 知识代码目录迁移

新库使用knowledge-schema.sql，再执行knowledge-data.sql。旧库执行unify-knowledge-code-locations.sql，不能用CREATE TABLE IF NOT EXISTS升级已有表。

迁移前备份knowledge_project、knowledge_service及knowledge_interface并暂停目录配置写入。脚本分两阶段：先给已有服务标记BACKEND，再将每条项目记录的目录迁入FRONTEND记录（包括空目录）。前端沿用原项目ID，service_code使用project_code；不生成新ID。核对目录逐项相同、后端ID不变及接口关联不变后，才删除项目表local_path。MySQL DDL隐式提交，脚本只执行一次，失败须按实际表结构恢复，不能盲目重跑。

统一表唯一键为(system_code, project_code, service_type, service_code)，前后端项目号和服务标识均可相同。service_type无默认值，后续写入必须明确指定FRONTEND或BACKEND。迁移期间部署配套后端代码，旧代码依赖已删除的列。


## 组件交互与联动

新库执行 knowledge-schema.sql（已包含两表）；已有库执行 component-interaction.sql 一次。脚本不使用 IF NOT EXISTS 掩盖结构不一致，不插入示例数据、不修改原表。执行前选择数据库，检查 knowledge_component 已存在及两张新表不存在；MySQL DDL 隐式提交，若第二张表创建失败，核对实际结构后只执行尚未完成的语句，不盲目重跑、不使用 --force。

- knowledge_component_interaction：一条用户操作或组件触发行为，component_id 为来源组件数据库主键。
- knowledge_component_relation：交互对目标组件的具体影响；通过 interaction_id 关联来源，target_component_id 为目标组件数据库主键。不另存反向关系。
- 新主键必须先通过项目 CUuid.nextId() 生成；更新沿用原 ID。action_name、effect_text 必须是非空白业务说明，sort_order 决定展示顺序。
- SQL 维护使用显式事务。写入前检查来源组件、交互及目标存在，两端属于同系统同项目，关系目标不同于来源。没有外键或级联，删除或变更组件、交互时须显式处理关联，不能留下孤立记录。
- 同一操作可有多个目标，同一目标可有不同生效条件。依赖来源和影响范围由查询生成，不分别写入两套列表。各查询仅返回显式登记、不执行自然语言条件、不推断间接影响。
- 生效条件未知时保留 NULL；trigger_condition 是操作条件，condition_text 是单条目标联动条件，不能互相覆盖。
- 索引依据：交互按 component_id 查询排序；关系按 interaction_id 查询来源影响，按 target_component_id 查询依赖。关联查询涉及六张基础表，通过两个有界方向筛选关系主键，再按组件和页面主键关联，并校验系统项目。需要结合实际数据 EXPLAIN，空表计划不代表容量或性能达标。

维护后可用下列只读 SQL 检查无效登记（正常应无结果）：

```sql
SELECT i.id FROM knowledge_component_interaction i
LEFT JOIN knowledge_component c ON c.id=i.component_id
WHERE c.id IS NULL OR TRIM(i.action_name)='';

SELECT r.id FROM knowledge_component_relation r
LEFT JOIN knowledge_component_interaction i ON i.id=r.interaction_id
LEFT JOIN knowledge_component s ON s.id=i.component_id
LEFT JOIN knowledge_page sp ON sp.id=s.page_id
LEFT JOIN knowledge_component t ON t.id=r.target_component_id
LEFT JOIN knowledge_page tp ON tp.id=t.page_id
WHERE i.id IS NULL OR s.id IS NULL OR t.id IS NULL OR sp.id IS NULL OR tp.id IS NULL
OR sp.system_code != tp.system_code OR sp.project_code != tp.project_code
OR s.id=t.id OR TRIM(r.effect_text)='';
```


### 当前前端真实资料登记

2026-11-16 已执行 component-interaction-data.sql，在当前 zhituo 页面登记 26 个组件、39 条交互、56 条联动。核心指标 component_id 为前端 MetricsStrip，名称为核心指标总览，保留原数据库主键；不再登记 kpiStrip 别名。详细依据及双向验证见 docs/component-interaction-registration.md。

全新库顺序：knowledge-schema.sql → knowledge-data.sql → component-interaction-data.sql。旧库先执行 component-interaction.sql 创建两表，再核对已有组件与资料，确认未登记本批数据后执行 component-interaction-data.sql。本次资料脚本使用固定的 CUuid 主键，事务内一次性写入；已执行的库不能重复执行，不能用 --force 忽略冲突。
