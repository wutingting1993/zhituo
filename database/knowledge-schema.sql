-- 知识模块完整DDL：当前最终结构，共14张表。
-- 用于新库初始化；不含业务数据、历史迁移、清表或覆盖操作。
-- 已存在的同名表不会自动升级，执行前选择目标数据库。
SET NAMES utf8mb4;

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
    name varchar(255) NULL COMMENT '页面名称',
    description text NULL COMMENT '业务功能描述',
    PRIMARY KEY (id), UNIQUE KEY uk_page (system_code, project_code, page_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识查询page';

CREATE TABLE IF NOT EXISTS knowledge_component (
    id varchar(64) NOT NULL COMMENT '主键由CUuid生成',
    page_id varchar(64) NOT NULL COMMENT '页面编号',
    component_id varchar(128) NOT NULL COMMENT '页面内组件标识',
    name varchar(255) NULL COMMENT '组件名称',
    description text NULL COMMENT '业务功能描述',
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
    name varchar(255) NULL COMMENT '接口名称',
    description text NULL COMMENT '接口用途',
    request_content_type varchar(128) NULL COMMENT '请求媒体类型',
    response_content_type varchar(128) NULL COMMENT '响应媒体类型',
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
    title varchar(255) NULL COMMENT '知识标题',
    knowledge_type varchar(16) NULL COMMENT 'BUSINESS业务 TECHNICAL技术',
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
    PRIMARY KEY(id), UNIQUE KEY uk_interface_field(interface_id,direction,location,field_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口field';

CREATE TABLE IF NOT EXISTS knowledge_interface_example (
    id varchar(64) NOT NULL COMMENT 'CUuid主键',
    interface_id varchar(64) NOT NULL COMMENT '接口编号',
    name varchar(255) NOT NULL COMMENT '示例名称',
    description text NOT NULL COMMENT '示例来源及适用说明',
    request_body text NOT NULL COMMENT '合法JSON请求值',
    response_body text NOT NULL COMMENT '合法JSON响应值',
    sort_order int NOT NULL COMMENT '排序序号',
    PRIMARY KEY(id), KEY idx_example_interface(interface_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口example';

CREATE TABLE IF NOT EXISTS knowledge_interface_error (
    id varchar(64) NOT NULL COMMENT 'CUuid主键',
    interface_id varchar(64) NOT NULL COMMENT '接口编号',
    code varchar(32) NOT NULL COMMENT '业务错误码',
    http_status int NOT NULL COMMENT 'HTTP状态',
    description text NOT NULL COMMENT '错误含义',
    trigger_condition text NOT NULL COMMENT '触发条件',
    sort_order int NOT NULL COMMENT '排序序号',
    PRIMARY KEY(id), UNIQUE KEY uk_interface_error(interface_id,code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化接口error';

CREATE TABLE IF NOT EXISTS knowledge_component_interaction (
    id varchar(64) NOT NULL COMMENT '交互主键由CUuid生成',
    component_id varchar(64) NOT NULL COMMENT '所属组件knowledge_component数据库主键',
    action_name varchar(255) NOT NULL COMMENT '用户操作或组件触发行为名称',
    trigger_condition text NULL COMMENT '操作触发条件，空表示未登记条件而非无条件',
    provided_content text NULL COMMENT '传递给关联组件的业务内容',
    response_text text NULL COMMENT '当前组件自身的响应或界面变化',
    sort_order int NOT NULL DEFAULT 0 COMMENT '所属组件内交互展示顺序',
    PRIMARY KEY (id),
    KEY idx_interaction_component (component_id,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件交互逻辑';

CREATE TABLE IF NOT EXISTS knowledge_component_relation (
    id varchar(64) NOT NULL COMMENT '联动关系主键由CUuid生成',
    interaction_id varchar(64) NOT NULL COMMENT '来源交互knowledge_component_interaction主键',
    target_component_id varchar(64) NOT NULL COMMENT '目标组件knowledge_component数据库主键',
    effect_text text NOT NULL COMMENT '操作对目标组件产生的具体影响',
    condition_text text NULL COMMENT '本条联动的生效条件，空表示未登记条件',
    sort_order int NOT NULL DEFAULT 0 COMMENT '同一交互下目标联动展示顺序',
    PRIMARY KEY (id),
    KEY idx_relation_interaction (interaction_id,sort_order,id),
    KEY idx_relation_target (target_component_id,interaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件有向联动关系';
