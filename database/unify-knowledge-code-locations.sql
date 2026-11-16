-- 适用于尚未迁移的旧知识表；仅执行一次，不使用 --force。
-- 先备份 knowledge_project、knowledge_service 和 knowledge_interface；暂停目录配置写入。
-- MySQL DDL 隐式提交，不可依赖事务回滚；删除旧列前必须核对下方校验查询返回0。
-- 第一阶段：保留已有后端ID，将原项目记录ID沿用为前端服务记录ID。
ALTER TABLE knowledge_service
    ADD COLUMN service_type varchar(16) NOT NULL DEFAULT 'BACKEND' COMMENT '代码类型FRONTEND前端BACKEND后端' AFTER service_code,
    MODIFY COLUMN local_path varchar(1024) NULL COMMENT '本地代码目录',
    DROP INDEX uk_service,
    ADD UNIQUE KEY uk_service (system_code, project_code, service_type, service_code);

INSERT INTO knowledge_service (id, system_code, project_code, service_code, service_type, local_path)
SELECT id, system_code, project_code, project_code, 'FRONTEND', local_path
FROM knowledge_project;

-- 应为0；目录用NULL安全比较，空目录也迁移以保留缺失提示。
SELECT COUNT(*) AS unmatched_frontend_locations
FROM knowledge_project p LEFT JOIN knowledge_service s
ON s.id=p.id AND s.system_code=p.system_code AND s.project_code=p.project_code
AND s.service_type='FRONTEND' AND s.service_code=p.project_code
WHERE s.id IS NULL OR NOT (s.local_path <=> p.local_path);

-- 第二阶段：仅在上述核对及后端ID、接口关联核对全部通过后执行。
ALTER TABLE knowledge_project DROP COLUMN local_path;
ALTER TABLE knowledge_service ALTER COLUMN service_type DROP DEFAULT;
