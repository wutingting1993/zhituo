-- 仅创建知识查询表，不修改现有表；已有表的结构需另行核对。
CREATE TABLE IF NOT EXISTS knowledge_project (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 system_code varchar(64) NOT NULL COMMENT '系统编号',
 project_code varchar(64) NOT NULL COMMENT '项目号',
 PRIMARY KEY (id), UNIQUE KEY uk_project (system_code, project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询project';

CREATE TABLE IF NOT EXISTS knowledge_service (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 system_code varchar(64) NOT NULL COMMENT '系统编号',
 project_code varchar(64) NOT NULL COMMENT '项目号',
 service_code varchar(64) NOT NULL COMMENT '服务标识',
    service_type varchar(16) NOT NULL COMMENT '代码类型FRONTEND前端BACKEND后端',
 local_path varchar(1024) NULL COMMENT '本地代码目录',
 PRIMARY KEY (id), UNIQUE KEY uk_service (system_code, project_code, service_type, service_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询service';

CREATE TABLE IF NOT EXISTS knowledge_page (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 system_code varchar(64) NOT NULL COMMENT '系统编号',
 project_code varchar(64) NOT NULL COMMENT '项目号',
 page_path varchar(255) NOT NULL COMMENT '页面路径',
 PRIMARY KEY (id), UNIQUE KEY uk_page (system_code, project_code, page_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询page';

CREATE TABLE IF NOT EXISTS knowledge_component (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 page_id varchar(64) NOT NULL COMMENT '页面编号',
 component_id varchar(128) NOT NULL COMMENT '页面内组件标识',
 PRIMARY KEY (id), UNIQUE KEY uk_component (page_id, component_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询component';

CREATE TABLE IF NOT EXISTS knowledge_flow (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 system_code varchar(64) NOT NULL COMMENT '系统编号',
 project_code varchar(64) NOT NULL COMMENT '项目号',
 name varchar(255) NOT NULL COMMENT '流程名称',
 PRIMARY KEY (id), KEY idx_flow_project (system_code, project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询flow';

CREATE TABLE IF NOT EXISTS knowledge_page_flow (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 page_id varchar(64) NOT NULL COMMENT '页面编号',
 flow_id varchar(64) NOT NULL COMMENT '流程编号',
 PRIMARY KEY (id), UNIQUE KEY uk_page_flow (page_id, flow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询page_flow';

CREATE TABLE IF NOT EXISTS knowledge_interface (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 service_id varchar(64) NOT NULL COMMENT '服务编号',
 method varchar(16) NOT NULL COMMENT 'HTTP方法大写',
 path varchar(255) NOT NULL COMMENT '实际接口路径或整段参数模板',
 PRIMARY KEY (id), UNIQUE KEY uk_interface (service_id, method, path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询interface';

CREATE TABLE IF NOT EXISTS knowledge_entry (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 system_code varchar(64) NOT NULL COMMENT '系统编号',
 project_code varchar(64) NOT NULL COMMENT '项目号',
 kind varchar(32) NOT NULL COMMENT '知识类别',
 content text NOT NULL COMMENT '知识正文',
 source text NULL COMMENT '知识来源',
 status varchar(16) NOT NULL COMMENT 'CONFIRMED已确认PENDING待确认EXPIRED失效',
 PRIMARY KEY (id), KEY idx_entry_project (system_code, project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询entry';

CREATE TABLE IF NOT EXISTS knowledge_applicability (
 id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
 knowledge_id varchar(64) NOT NULL COMMENT '知识编号',
 target_type varchar(16) NOT NULL COMMENT 'COMPONENT PAGE FLOW INTERFACE',
 target_id varchar(64) NOT NULL COMMENT '关联对象主键',
 condition_text text NULL COMMENT '适用条件未知时为空',
 unconditional tinyint NOT NULL COMMENT '是否明确无条件适用',
 exception_text text NULL COMMENT '明确例外及已知冲突依据',
 replaces_knowledge_id varchar(64) NULL COMMENT '明确替代的知识编号',
 PRIMARY KEY (id), KEY idx_applicability_target (target_type, target_id), KEY idx_applicability_knowledge (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询applicability';
