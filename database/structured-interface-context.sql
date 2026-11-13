-- 增量迁移：只增加结构化接口知识字段与表。既有知识分类需按逐条确认的数据迁移；未知历史分类保持NULL并提示，不擅自归类。
SET NAMES utf8mb4;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_interface' AND COLUMN_NAME='name')=0, 'ALTER TABLE knowledge_interface ADD COLUMN name varchar(255) NULL COMMENT ''接口名称''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_interface' AND COLUMN_NAME='description')=0, 'ALTER TABLE knowledge_interface ADD COLUMN description text NULL COMMENT ''接口用途''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_interface' AND COLUMN_NAME='request_content_type')=0, 'ALTER TABLE knowledge_interface ADD COLUMN request_content_type varchar(128) NULL COMMENT ''请求媒体类型''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_interface' AND COLUMN_NAME='response_content_type')=0, 'ALTER TABLE knowledge_interface ADD COLUMN response_content_type varchar(128) NULL COMMENT ''响应媒体类型''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_entry' AND COLUMN_NAME='title')=0, 'ALTER TABLE knowledge_entry ADD COLUMN title varchar(255) NULL COMMENT ''知识标题''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_entry' AND COLUMN_NAME='knowledge_type')=0, 'ALTER TABLE knowledge_entry ADD COLUMN knowledge_type varchar(16) NULL COMMENT ''BUSINESS业务 TECHNICAL技术''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_page' AND COLUMN_NAME='name')=0, 'ALTER TABLE knowledge_page ADD COLUMN name varchar(255) NULL COMMENT ''页面名称''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='knowledge_component' AND COLUMN_NAME='name')=0, 'ALTER TABLE knowledge_component ADD COLUMN name varchar(255) NULL COMMENT ''组件名称''', 'SELECT 1');
PREPARE knowledge_ddl FROM @ddl;
EXECUTE knowledge_ddl;
DEALLOCATE PREPARE knowledge_ddl;
CREATE TABLE IF NOT EXISTS knowledge_interface_field (
 id varchar(64) NOT NULL COMMENT 'CUuid主键',
interface_id varchar(64) NOT NULL COMMENT '接口编号',
direction varchar(16) NOT NULL COMMENT 'REQUEST入参 RESPONSE出参',
location varchar(16) NOT NULL COMMENT '参数位置',
parent_id varchar(64) NULL COMMENT '父字段编号',
field_name varchar(128) NOT NULL COMMENT '字段名称',
field_path varchar(255) NOT NULL COMMENT '完整字段路径',
value_type varchar(64) NOT NULL COMMENT '数据类型',
item_type varchar(64) NULL COMMENT '数组元素类型',
required_flag tinyint NOT NULL COMMENT '是否必须存在',
nullable_flag tinyint NOT NULL COMMENT '是否允许空值',
business_meaning text NULL COMMENT '业务含义',
technical_description text NULL COMMENT '技术说明',
validation_text text NULL COMMENT '校验要求',
sort_order int NOT NULL COMMENT '排序序号',
 PRIMARY KEY(id), UNIQUE KEY uk_interface_field(interface_id,direction,location,field_path)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口field';
CREATE TABLE IF NOT EXISTS knowledge_interface_example (
 id varchar(64) NOT NULL COMMENT 'CUuid主键',
interface_id varchar(64) NOT NULL COMMENT '接口编号',
name varchar(255) NOT NULL COMMENT '示例名称',
description text NOT NULL COMMENT '示例来源及适用说明',
request_body text NOT NULL COMMENT '合法JSON请求值',
response_body text NOT NULL COMMENT '合法JSON响应值',
sort_order int NOT NULL COMMENT '排序序号',
 PRIMARY KEY(id), KEY idx_example_interface(interface_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口example';
CREATE TABLE IF NOT EXISTS knowledge_interface_error (
 id varchar(64) NOT NULL COMMENT 'CUuid主键',
interface_id varchar(64) NOT NULL COMMENT '接口编号',
code varchar(32) NOT NULL COMMENT '业务错误码',
http_status int NOT NULL COMMENT 'HTTP状态',
description text NOT NULL COMMENT '错误含义',
trigger_condition text NOT NULL COMMENT '触发条件',
sort_order int NOT NULL COMMENT '排序序号',
 PRIMARY KEY(id), UNIQUE KEY uk_interface_error(interface_id,code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口error';
