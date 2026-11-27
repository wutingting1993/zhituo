-- 仅用于本机监控页面验收：关闭、开启但零命中、配置未知、开启且有命中。
-- 仅调整本机 local-iface-1/2/3/4 样例，不修改其他接口，不启用接口调用。
START TRANSACTION;
INSERT INTO api_interface
    (id, name, description, method, url, auth_type, status,
     created_by_id, created_by_name, modified_by_id, modified_by_name,
     version, published_version, cache_enabled)
VALUES
    ('local-iface-4', '缓存命中验证查询', '本机监控缓存命中样例', 'GET',
     'http://127.0.0.1/local-monitor-cache-fixture', 'NONE', 'DISABLED',
     'local-monitor', '本机监控样例', 'local-monitor', '本机监控样例', 0, 1, 1)
ON DUPLICATE KEY UPDATE cache_enabled = 1;

INSERT INTO api_interface_version
    (id, interface_id, version_no, draft_revision, name, method, url, auth_type,
     description, published_by_id, published_by_name, cache_enabled, cache_ttl_minutes)
SELECT
    CASE id WHEN 'local-iface-1' THEN 'local-cache-01'
        WHEN 'local-iface-2' THEN 'local-cache-02' ELSE 'local-cache-04' END,
    id, 1, version, name, method, url, auth_type,
    '本机监控缓存状态验收样例', 'local-monitor', '本机监控样例',
    IF(id = 'local-iface-1', 0, 1), 5
FROM api_interface
WHERE id IN ('local-iface-1', 'local-iface-2', 'local-iface-4')
ON DUPLICATE KEY UPDATE cache_enabled = VALUES(cache_enabled);

UPDATE api_interface
SET published_version = 1, cache_enabled = IF(id = 'local-iface-1', 0, 1)
WHERE id IN ('local-iface-1', 'local-iface-2', 'local-iface-4');

UPDATE api_interface SET published_version = 0 WHERE id = 'local-iface-3';
COMMIT;
