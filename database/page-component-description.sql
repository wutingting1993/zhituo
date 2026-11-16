-- 页面与组件业务描述，只新增必要字段并补充当前案例。
SET NAMES utf8mb4;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_page' AND COLUMN_NAME='description')=0,'ALTER TABLE knowledge_page ADD COLUMN description text NULL COMMENT ''业务功能描述''','SELECT 1');
PREPARE description_ddl FROM @ddl;
EXECUTE description_ddl;
DEALLOCATE PREPARE description_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_component' AND COLUMN_NAME='description')=0,'ALTER TABLE knowledge_component ADD COLUMN description text NULL COMMENT ''业务功能描述''','SELECT 1');
PREPARE description_ddl FROM @ddl;
EXECUTE description_ddl;
DEALLOCATE PREPARE description_ddl;
START TRANSACTION;
UPDATE knowledge_page SET description='提供规划与执行报表的检索和统计分析，可按日期、分行、支行、客户经理等级及技能查看相关数据。' WHERE system_code='LQ13.03' AND project_code='zhituo' AND page_path='/Users/natasha/Downloads/智拓报表.html';
UPDATE knowledge_component c JOIN knowledge_page p ON p.id=c.page_id SET c.description='位于检索框下方，通过统计接口获取当前筛选范围的规划数、技能数、任务或客户方案数、推荐客户数、方案查看率和接触率，并提供覆盖支行、人均数量、最广使用技能及机构基准比较。' WHERE p.system_code='LQ13.03' AND p.project_code='zhituo' AND p.page_path='/Users/natasha/Downloads/智拓报表.html' AND c.component_id='MetricsStrip';
COMMIT;
