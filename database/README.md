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

## 知识服务

知识 SQL、迁移说明及验证资料已迁至 ../component-knowledge/database，现有知识表保持不变。
