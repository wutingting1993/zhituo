-- 用户于2026-11-13明确修正：仅业务功能及接口口径；页面数据按接口获取。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE knowledge_entry SET content='检索框下的指标统计组件ID为kpiStrip，数据通过统计接口获取。统计内容包括生效中规划、涉及技能、当日或周期任务数、推荐客户数、方案被查看率和接触率，以及覆盖支行、使用人数最多技能、人均数量和机构基准差值。' WHERE id='328937da8e01ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET content='指标统计按当前日期区间、分行、支行、客户经理等级和技能条件调用接口获取结果。单日与多日分别对应当日和周期统计；机构基准用于比较当前范围与所选分行整体水平。' WHERE id='328937da8e02ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET content='当前summary的planCount按执行统计范围中的规划编号去重，coveredSubBranchCount按分行编号与支行编号组合去重。页面业务名称为生效中规划，当前后端不额外按发布状态、有效期起止或当前生效状态过滤，也不套用规划覆盖模块的三类来源限制。' WHERE id='328937da8e05ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET content='方案被查看客户数为当前范围is_read=1执行记录对应的成功推荐客户标识去重数。viewRate=方案被查看客户数/推荐客户数×100；推荐客户数为0时为null。' WHERE id='328937da8e09ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET content='页面业务说明与当前后端契约存在待确认的口径差异：页面生效中规划说明按当前状态、不受日期影响且按规划负责人机构归集；后端按排程及执行范围、目标客户经理机构，不额外限制生效状态。页面任务数解释为下发任务，后端taskCount表示成功推荐客户方案数，另有scheduledTaskCount等任务指标。页面接触解释为有效接通、到店或线上互动，后端按接触明细关联去重，不附加有效接通条件。页面数据按接口获取，聚合与去重以接口业务契约为依据。后续需求澄清应明确这些业务口径的取舍，不自动修改现有实现。' WHERE id='328937da8e0fed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET kind='BUSINESS_RULE' WHERE id='328937da8e01ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_entry SET kind='BUSINESS_RULE' WHERE id='328937da8e02ed87' AND system_code='LQ13.03' AND project_code='zhituo';
UPDATE knowledge_applicability SET exception_text='页面业务说明与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。' WHERE knowledge_id='328937da8e0fed87' AND exception_text='原型事实与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。';
COMMIT;
