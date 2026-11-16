-- 组件交互及有向联动：只新增表，不修改已有知识或报表数据。
-- 先选择目标库并核对同名表；MySQL DDL 隐式提交，不使用 --force。
SET NAMES utf8mb4;

CREATE TABLE knowledge_component_interaction (
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

CREATE TABLE knowledge_component_relation (
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
