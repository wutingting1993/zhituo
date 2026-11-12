-- 技能分类配置；仅在确认目标库尚无该表后执行，不覆盖已有表。
CREATE TABLE `swe_marketplace_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `source_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用入口标识',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，升序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `branch_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '分行是否可见',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_name` (`source_id`,`name`),
  KEY `idx_source_id` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='市场技能分类配置';
