-- 用户明确授权不保留本案例迁移前旧数据；只删除旧契约和拆分前知识，不清空当前有效知识或报表业务表。
SET NAMES utf8mb4;
START TRANSACTION;
DELETE a FROM knowledge_applicability a JOIN knowledge_entry e ON e.id=a.knowledge_id WHERE e.system_code='LQ13.03' AND e.project_code='zhituo' AND e.kind IN ('API_CONTRACT_LEGACY','MIXED_LEGACY');
DELETE FROM knowledge_entry WHERE system_code='LQ13.03' AND project_code='zhituo' AND kind IN ('API_CONTRACT_LEGACY','MIXED_LEGACY');
COMMIT;
