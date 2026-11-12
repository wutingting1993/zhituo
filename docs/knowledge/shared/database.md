# 数据库表说明与数据链路

归档日期：2026-11-12。依据仓库建表SQL和当前Mapper，不代表已核对任何环境实时结构。字段字典来自DDL；实际统计条件以共享规则和当前实现为准，不能仅凭字段注释推断过滤规则。

## 业务链路

规划 → 分发目标客户经理 → 场景技能 → 客户经理任务 → 排程日期匹配实际执行 → 成功结果 → 点击行为 → 接触明细。技能分类用于展示。

排程是计划日期，任务是执行定义，执行是实际发生记录，结果是执行产出；报表“任务数”另按成功结果定义。

## 表关系与关联基数

以下是业务关联，不是数据库外键。1:N中的N允许为0；无外键时也可能存在孤立记录。业务预期和DDL实际唯一性分别说明，不能仅凭字段名认定1:1。

| 表关系 | 关联字段 | 基数与约束说明 |
| --- | --- | --- |
| 规划 → 目标 | plans.id = targets.plan_id | 1:N；目标仅id唯一，未约束(plan_id,sap_id)唯一，同一客户经理可能重复出现 |
| 规划 → 场景 | plans.id = scenes.plan_id | 1:N；未约束(plan_id,scene_id)唯一，场景明细id与技能编号不同 |
| 规划 → 排程 | plans.id = cron.plan_id | 1:N；(plan_id,sence_id,execute_day)唯一，同一规划技能可跨多个日期 |
| 场景 → 排程 | scenes.plan_id/scene_id = cron.plan_id/sence_id | 业务为某规划技能对应多个日期；因场景组合不唯一，原始行关联可能M:N，不是场景id外键 |
| 目标 → 客户经理信息 | targets.sap_id = user_info.user_id | 业务预期目标定位客户经理；DDL客户经理主键为(first_bbk_id,org_id,user_id)，仅user_id匹配可能多行，整体可M:N |
| 场景 → 技能目录 | scenes.scene_id = skills.skill_id | 业务为场景引用技能；skill_id仅普通索引，按source_id=RMASSIST仍未保证唯一，原始关联可能M:N |
| 规划 → 任务 | plans.id = jobs.plan_id | 1:N；一规划可分发多个客户经理和技能任务 |
| 目标/场景 → 任务 | plan_id、targets.sap_id=jobs.tenant_id、scenes.scene_id=jobs.skill_ids | 按组合匹配，未声明该组合唯一；不可认定一客户经理一规划一技能只有一个任务。skill_ids当前整字段匹配，不拆分 |
| 任务 → 执行 | jobs.id = executions.job_id | 1:N；同日可多次执行，报表再选当天最新，不是底层表1:1 |
| 排程 → 执行 | 规划技能匹配任务，execute_day匹配actual_time当天 | 通过任务间接关联；一个排程可对应多个客户经理任务和执行。执行无排程id外键，不假定每条执行只能命中一条原始排程链路 |
| 执行 → 结果索引 | job_id、skill_id、trace_id共同匹配 | 业务为一次执行多个结果；trace_id非唯一且可空，按当前查询不保证原始执行行与结果为严格1:N，可能M:N。结果虽有execution_id，当前Mapper不以此关联 |
| 结果 → 点击 | trace_id、custuid=customer_id、执行当天 | 一个客户结果可有多次点击；结果组合和点击组合均不唯一，原始关联可M:N，需按口径去重 |
| 点击 → 接触明细 | clicks.id = detail.click_id | 1:N；接触主键(click_id,cont_srl_nbr)，允许同一次点击关联多个流水 |
| 分类 → 技能 | categories.id = skills.category_id，且source_id相同 | 1:N；每技能记录最多匹配一个分类，允许未配或缺失分类；同技能业务编号可能多条目录记录 |

其中plans、targets、scenes、cron、jobs、executions、skills、user_info、clicks、detail、categories分别指下方完整表名对应的表；结果索引为swe_cron_result_index。

## 数据产生时机与生命周期

本仓库是报表只读消费端，Mapper未提供这些业务表的生产写入流程。排程生成规则已由用户确认；其余下表内容是根据DDL字段注释与报表需求可识别的业务阶段，**不是已核实的上游事务顺序**。具体由哪个系统写入、是否同步、重试及更新策略尚需上游代码或业务确认，不把推断当成事实。

| 表 | 对应业务阶段与产生/更新含义 | 已知依据与未知边界 |
| --- | --- | --- |
| swe_wealth_plans | 创建规划时形成主记录；发布过程可更新状态、发布结果，删除可更新deleted | DDL有created_at、published_at、status及deleted；实际写入事务未提供 |
| swe_wealth_plan_targets | 配置/保存规划分发对象时形成目标客户经理关系 | DDL和需求定义分发目标；是在保存还是发布时落库未核实 |
| swe_wealth_plan_scenes | 配置规划技能时形成场景记录，发布后可回填任务标识 | cron_job_id等注释写明发布后回填；与客户经理任务的具体生成顺序未核实 |
| swe_plan_sence_cron | 定时任务基于场景表 swe_wealth_plan_scenes.cron_expr，在T日生成T+1日的排程数据，execute_day为T+1日 | 用户于2026-11-12明确确认；具体触发时刻、时区、补跑及幂等实现未提供 |
| swe_cron_jobs | 创建/发布或分发定时任务时形成任务定义，后续可暂停、删除 | 字段包含enabled、status、cron_expr等；报表不负责触发创建 |
| swe_cron_executions | 任务实际触发执行后形成执行历史，结束、状态与查看信息可后续更新 | actual_time、end_time、status、is_read/read_at；不保证执行记录已出现时结果已就绪 |
| swe_cron_result_index | 子任务产出可查询结果时建立结果索引，状态/最新成功标记可更新 | execution_at、created_at、status、is_latest_success；失败/重试具体写入时点未核实 |
| swe_html_preview_click_events | 发生预览或按钮点击时记录事件 | clicked_at表示事件时间，不能据此保证实时入库或只上报一次 |
| swe_skill_contact_detail | 接触明细产生并关联点击后记录接触流水 | cont_dt、cont_start_tm与click_id；同步/异步回流方式和时延未核实，报表不额外限制接触日期 |
| swe_marketplace_skills | 技能目录维护时产生或更新 | created_at、updated_at、分类及上下架字段；不假定每次执行新建技能 |
| swe_marketplace_categories | 维护分类配置时产生或更新 | 字典结构及分类验证记录；不假定随任务执行生成 |
| rm_jkh_user_info | 客户经理基础信息维护/同步时产生或更新 | 表注释为实时表且有update_time；同步周期及历史保留策略未核实 |

阶段示例：规划配置 → 发布/任务与日期安排 → 实际执行 → 结果产出 → 客户经理查看或点击 → 接触明细关联。该箭头帮助理解业务依赖，不承诺跨表落库严格依次完成；查看与不同按钮行为不必逐级发生。已有执行可能无成功结果，有推荐可能无点击，有点击可能无接触记录，均不能补造数据。

## 表清单及字段字典

### swe_wealth_plans

规划主表：规划名称、创建人、所属机构、来源及展示有效期。创建人不等于分发目标客户经理。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | varchar(64) NOT NULL | 规划ID（后端生成） |
| `sap_id` | varchar(128) NOT NULL | 创建人SAP工号（X-User-Id） |
| `creator_name` | varchar(128) DEFAULT NULL | 创建人姓名（冗余展示） |
| `bbk_id` | varchar(64) DEFAULT NULL | 创建人所在分行号 |
| `source_id` | varchar(128) DEFAULT NULL | 来源标识（iframeStore.source，如 RMASSIST） |
| `agent_id` | varchar(64) DEFAULT NULL | 预留：关联Agent ID，当前可为空 |
| `name` | varchar(255) NOT NULL | 规划名称 |
| `description` | text | 规划说明 |
| `source_label` | varchar(32) DEFAULT NULL | 来源标签：我的关注/行长关注/分行关注（按创建人角色派生）；分行关注的当前历史存储值为“分行中台”，原DDL注释沿用该存储名称 |
| `period_start` | varchar(16) DEFAULT NULL | 规划周期开始日期 YYYY-MM-DD |
| `period_end` | varchar(16) DEFAULT NULL | 规划周期结束日期 YYYY-MM-DD |
| `status` | varchar(32) NOT NULL DEFAULT 'publishing' | 发布编排状态：publishing/published/publish_failed |
| `publish_error` | text | 发布失败原因 |
| `skill_dispatch_task_id` | varchar(64) DEFAULT NULL | 技能/MCP 批量分发批次ID（batch_id，非任务ID） |
| `skill_dispatch_status` | varchar(32) DEFAULT NULL | 技能/MCP 分发状态 |
| `skill_dispatch_error` | text | 技能/MCP 分发失败原因 |
| `created_at` | timestamp NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `published_at` | datetime DEFAULT NULL | 发布完成时间 |
| `bbk_org_id` | varchar(64) DEFAULT NULL | 所属支行号 |
| `deleted` | tinyint NOT NULL DEFAULT '0' | 逻辑删除标记：0-未删除 1-已删除 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_sap` (`sap_id`)`
- `KEY `idx_bbk` (`bbk_id`)`

### swe_wealth_plan_scenes

规划包含的场景技能。scene_id 是技能业务编号；id 是场景明细自增主键，两者不能混用。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 主键ID |
| `plan_id` | varchar(64) NOT NULL | 关联规划ID |
| `scene_id` | varchar(64) NOT NULL | 场景技能ID（skillId） |
| `item_id` | varchar(64) DEFAULT NULL | 外部场景接口 itemId（发布回传用） |
| `scene_name` | varchar(255) NOT NULL | 场景名称（冗余存储，不依赖场景池展示） |
| `category` | varchar(32) NOT NULL | 产品大类英文code：insurance/finance/deposit/payroll/cross_border/fund |
| `direction` | varchar(512) DEFAULT NULL | 经营方向 |
| `cycle` | varchar(16) DEFAULT NULL | 任务周期：本月/本季/今日/T+1日/自定义 |
| `start_date` | varchar(16) DEFAULT NULL | 场景有效期开始日期 YYYY-MM-DD |
| `end_date` | varchar(16) DEFAULT NULL | 场景有效期结束日期 YYYY-MM-DD |
| `cron_expr` | varchar(64) NOT NULL | 由排程选择换算的cron表达式（建定时任务用） |
| `cron_example` | varchar(2048) DEFAULT NULL | 场景技能自带的cron示例文本（定时任务请求内容来源） |
| `mcp_relations` | varchar(512) DEFAULT NULL | MCP依赖列表，逗号分隔存储 |
| `cron_job_id` | varchar(64) DEFAULT NULL | 发布后回填：定时任务ID |
| `broadcast_task_id` | varchar(64) DEFAULT NULL | 发布后回填：广播分发任务ID（看板状态来源） |
| `sort_order` | int NOT NULL DEFAULT '0' | 展示顺序 |
| `cust_num` | int DEFAULT NULL | 限制名单数，空表示不限制 |
| `monitor_schedule` | tinyint DEFAULT NULL | 督办监控周期 1- 当日,2-T+7日内 |
| `monitor_cron` | varchar(64) DEFAULT NULL | 督办cron表达式 |
| `monitor_finish_rate` | int DEFAULT NULL | 督办预计完成率 百分比值 |
| `deleted` | tinyint NOT NULL DEFAULT '0' | 逻辑删除标记：0-未删除 1-已删除 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_plan` (`plan_id`)`

### swe_wealth_plan_targets

规划分发给哪些客户经理；sap_id 连接客户经理 user_id，决定统计机构和等级。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 主键ID |
| `plan_id` | varchar(64) NOT NULL | 关联规划ID |
| `sap_id` | varchar(64) NOT NULL | 分发目标客户经理SAP工号 |
| `target_name` | varchar(128) DEFAULT NULL | 目标姓名（冗余展示） |
| `sort_order` | int NOT NULL DEFAULT '0' | 展示顺序 |
| `deleted` | tinyint NOT NULL DEFAULT '0' | 逻辑删除标记：0-未删除 1-已删除 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_plan` (`plan_id`)`
- `KEY `idx_sap` (`sap_id`)`

### swe_plan_sence_cron

排程：规划中的技能安排在哪一天执行。不是实际执行结果。execute_day 是报表日期；plan_id、sence_id、execute_day 组合唯一。原始 sence 拼写保留。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 自增主键 |
| `plan_id` | varchar(64) COLLATE utf8mb4_bin NOT NULL | 关联规划ID |
| `sence_id` | varchar(64) COLLATE utf8mb4_bin NOT NULL | 场景技能ID（skillId） |
| `execute_day` | varchar(30) COLLATE utf8mb4_bin NOT NULL | 执行日期 YYYY-MM-DD |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_plan_sence_day` (`plan_id`,`sence_id`,`execute_day`) COMMENT '同一规划场景同一执行日只保留一条'`
- `KEY `idx_execute_day` (`execute_day`)`

### swe_cron_jobs

客户经理、规划、技能对应的定时任务定义。tenant_id 在此链路匹配目标客户经理工号；skill_ids 按整字段匹配，不拆成数组。表中一条任务不等于报表任务数。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | varchar(64) NOT NULL | 任务ID (UUID) |
| `name` | varchar(255) NOT NULL | 任务名称 |
| `tenant_id` | varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL | 客户经理/租户ID |
| `tenant_name` | varchar(50) DEFAULT '' | 租户姓名 (X-User-Name header) |
| `bbk_id` | varchar(64) DEFAULT '' | 分行号 (X-Bbk-Id header) |
| `source_id` | varchar(64) DEFAULT '' | 来源标识 (X-Source-Id header) |
| `enabled` | tinyint(1) DEFAULT '1' | 是否启用 |
| `task_type` | varchar(16) NOT NULL | 任务类型: text/agent |
| `cron_expr` | varchar(64) NOT NULL | cron表达式 (5字段) |
| `timezone` | varchar(32) DEFAULT 'UTC' | 时区 |
| `channel` | varchar(32) NOT NULL | 分发渠道 |
| `target_user_id` | varchar(64) DEFAULT '' | 目标用户ID |
| `target_session_id` | varchar(64) DEFAULT '' | 目标会话ID |
| `timeout_seconds` | int DEFAULT '7200' | 超时秒数 |
| `max_concurrency` | int DEFAULT '1' | 最大并发数 |
| `misfire_grace_seconds` | int DEFAULT '300' | misfire容错秒数 |
| `text_content` | varchar(4096) DEFAULT '' | text类型任务内容 |
| `request_input` | varchar(4096) DEFAULT '' | agent类型请求输入 |
| `creator_user_id` | varchar(64) DEFAULT '' | 创建者用户ID |
| `task_chat_id` | varchar(64) DEFAULT '' | 关联聊天ID |
| `task_session_id` | varchar(64) DEFAULT '' | 关联会话ID |
| `meta` | varchar(4096) DEFAULT '' | 扩展元数据 |
| `broadcast_source_job_id` | varchar(64) DEFAULT NULL | 源定时任务ID |
| `status` | varchar(16) DEFAULT 'active' | 状态: active/paused/deleted |
| `pause_reason` | varchar(32) DEFAULT '' | 暂停原因 |
| `created_at` | datetime DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `deleted_at` | datetime DEFAULT NULL | 删除时间 |
| `job_origin` | varchar(32) NOT NULL DEFAULT 'manual' | 任务来源：manual=自主创建，subscription=订阅任务，system=系统任务 |
| `subscription_key` | varchar(255) DEFAULT NULL | 订阅任务稳定分组ID，仅订阅任务有值 |
| `skill_ids` | varchar(200) DEFAULT '' | 绑定技能ID，多个技能ID使用英文逗号分隔 |
| `plan_id` | varchar(255) DEFAULT NULL | 关联计划ID |
| `workflow_bind` | varchar(64) DEFAULT NULL | Workflow 绑定ID |
| `workflow_bind_id` | varchar(64) DEFAULT NULL | Workflow 绑定ID |
| `workflow_binding_id` | varchar(64) DEFAULT NULL | Workflow 绑定ID |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_tenant_id` (`tenant_id`)`
- `KEY `idx_bbk_id` (`bbk_id`)`
- `KEY `idx_source_id` (`source_id`)`
- `KEY `idx_creator_user_id` (`creator_user_id`)`
- `KEY `idx_status` (`status`)`
- `KEY `idx_enabled` (`enabled`)`
- `KEY `idx_created_at` (`created_at`)`
- `KEY `idx_bbk_source` (`bbk_id`,`source_id`)`

### swe_cron_result_index

名单任务和客户方案任务的执行结果索引，结果类型由result_type区分，不通过custuid是否为空判定。custuid用于记录结果对应的客户UID：名单任务不记录客户UID，字段为空字符串；客户方案任务记录客户UID，字段非空。报表任务数来自符合条件的结果去重，不直接数任务表行数。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 主键ID |
| `source_id` | varchar(64) NOT NULL DEFAULT '' | 来源ID |
| `tenant_id` | varchar(64) NOT NULL | 客户经理/租户ID |
| `first_bbk_id` | varchar(64) NOT NULL | 一级分行号 |
| `bbk_org_id` | varchar(64) NOT NULL | 二级分行号 |
| `custuid` | varchar(64) NOT NULL DEFAULT '' | 客户ID |
| `cust_nm` | varchar(255) DEFAULT NULL | 客户名称 |
| `skill_id` | varchar(64) NOT NULL | 技能ID |
| `job_id` | varchar(64) NOT NULL | 定时任务ID |
| `execution_id` | bigint NOT NULL | 执行记录ID |
| `trace_id` | varchar(64) NOT NULL | 执行trace_id |
| `subtask_id` | bigint NOT NULL | subtask主键 |
| `task_id` | varchar(128) NOT NULL | 异步子任务ID |
| `result_type` | varchar(16) NOT NULL | list/plan |
| `template_id` | bigint DEFAULT NULL | HTML模板ID |
| `result_id` | varchar(100) DEFAULT NULL | 待填充数据ID |
| `filename` | varchar(512) DEFAULT NULL | 文件名 |
| `status` | varchar(16) DEFAULT NULL | 子任务状态 |
| `is_latest_success` | tinyint(1) NOT NULL DEFAULT '1' | 是否为最新成功结果 |
| `execution_at` | datetime NOT NULL | 执行成功时间 |
| `created_at` | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime DEFAULT NULL | 更新时间 |
| `expire_at` | datetime NOT NULL | 过期时间 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_subtask_skill` (`subtask_id`,`skill_id`)`
- `KEY `idx_plan_query` (`source_id`,`tenant_id`,`first_bbk_id`,`bbk_org_id`,`custuid`,`result_type`,`is_latest_success`,`execution_at` DESC,`id` DESC)`
- `KEY `idx_list_query` (`source_id`,`tenant_id`,`first_bbk_id`,`bbk_org_id`,`result_type`,`is_latest_success`,`execution_at` DESC,`id` DESC)`
- `KEY `idx_skill_scope` (`source_id`,`tenant_id`,`first_bbk_id`,`skill_id`,`result_type`,`is_latest_success`,`execution_at` DESC)`
- `KEY `idx_expire_at` (`expire_at`)`
- `KEY `idx_job_latest` (`job_id`,`is_latest_success`,`execution_at` DESC)`
- `KEY `idx_plan_latest_skill_customer` (`source_id`,`tenant_id`,`first_bbk_id`,`bbk_org_id`,`skill_id`,`result_type`,`custuid`,`is_latest_success`)`
- `KEY `idx_plan_skill_latest` (`source_id`,`first_bbk_id`,`custuid`,`result_type`,`is_latest_success`,`expire_at`,`skill_id`)`

### swe_cron_executions

任务实际执行记录；job_id 对应任务，actual_time 是实际执行时间，trace_id 连接执行结果与行为，is_read 用于查看及参与统计。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 执行记录ID |
| `job_id` | varchar(64) NOT NULL | 任务ID |
| `job_name` | varchar(255) DEFAULT '' | 任务名称 (冗余存储便于查询) |
| `tenant_id` | varchar(64) NOT NULL | 租户ID (分行号) |
| `scheduled_time` | datetime DEFAULT NULL | 计划执行时间 |
| `actual_time` | datetime NOT NULL | 实际开始时间 |
| `end_time` | datetime DEFAULT NULL | 结束时间 |
| `duration_ms` | int DEFAULT '0' | 执行耗时 (毫秒) |
| `status` | varchar(16) NOT NULL | 状态: success/error/cancelled/timeout/skipped |
| `async_status` | varchar(16) DEFAULT NULL | 异步任务执行状态: success/error |
| `need_notification` | tinyint(1) DEFAULT '0' | 是否需要通知: 0-否, 1-是 |
| `error_message` | varchar(2048) DEFAULT '' | 错误信息 |
| `instance_id` | varchar(64) DEFAULT '' | 执行实例标识 |
| `executor_leader` | varchar(64) DEFAULT '' | 执行者 leader ID |
| `is_manual` | tinyint(1) DEFAULT '0' | 是否手动触发 |
| `trace_id` | varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL | 执行链路标识 |
| `session_id` | varchar(64) DEFAULT '' | 关联的 session ID |
| `input_snapshot` | text | 任务输入快照 |
| `output_preview` | varchar(512) DEFAULT '' | 输出预览 (前100字符) |
| `meta` | varchar(2048) DEFAULT '' | 执行元数据 |
| `dispatch_intent_id` | bigint DEFAULT NULL | 批调度派发意图ID |
| `dispatch_batch_id` | varchar(64) DEFAULT '' | 批调度批次ID |
| `dispatch_attempt` | int DEFAULT NULL | 批调度派发尝试次数 |
| `is_final` | tinyint(1) NOT NULL DEFAULT '1' | 是否为当前有效执行记录：1有效，0已被后续执行替代 |
| `notification_status` | varchar(16) DEFAULT 'not_required' | 通知状态 |
| `notification_due_at` | datetime DEFAULT NULL | 计划通知时间，北京时间 |
| `notification_timezone` | varchar(64) DEFAULT '' | 通知来源时区标记 |
| `notification_sent_at` | datetime DEFAULT NULL | 通知发送时间，北京时间 |
| `notification_attempts` | int DEFAULT '0' | 通知尝试次数 |
| `notification_error` | varchar(2048) DEFAULT '' | 通知错误 |
| `notification_lock_owner` | varchar(128) DEFAULT '' | 通知锁持有者 |
| `notification_locked_at` | datetime DEFAULT NULL | 通知锁时间，北京时间 |
| `created_at` | datetime DEFAULT CURRENT_TIMESTAMP | 记录创建时间 |
| `is_read` | tinyint(1) DEFAULT '0' | 是否已读（仅成功执行的任务） |
| `read_at` | datetime DEFAULT NULL | 已读时间 |
| `error_type` | varchar(2048) DEFAULT NULL | 子任务错误类型，逗号分隔 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_job_id` (`job_id`)`
- `KEY `idx_tenant_id` (`tenant_id`)`
- `KEY `idx_status` (`status`)`
- `KEY `idx_scheduled_time` (`scheduled_time`)`
- `KEY `idx_actual_time` (`actual_time`)`
- `KEY `idx_trace_id` (`trace_id`)`
- `KEY `idx_tenant_actual` (`tenant_id`,`actual_time`)`
- `KEY `idx_created_at` (`created_at`)`
- `KEY `idx_notification_scan` (`notification_status`,`notification_due_at`)`
- `KEY `idx_notification_lock` (`notification_lock_owner`,`notification_locked_at`)`
- `KEY `idx_cron_execution_dispatch` (`dispatch_intent_id`,`dispatch_batch_id`,`dispatch_attempt`)`
- `KEY `idx_job_status_async_trace` (`job_id`,`status`,`async_status`,`trace_id`)`

### swe_html_preview_click_events

预览及按钮点击事件；trace_id、customer_id、clicked_at 用于匹配某次执行当天的推荐客户行为。洞察和电访是点击行为。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 主键ID |
| `source_id` | varchar(64) DEFAULT NULL | 来源标识 |
| `user_id` | varchar(128) DEFAULT NULL | 点击用户标识 |
| `user_name` | varchar(255) DEFAULT NULL | 点击用户名称/客户经理姓名 |
| `bbk_id` | varchar(128) DEFAULT NULL | 分行/机构标识，预留给运营看板筛选 |
| `cron_task_id` | varchar(128) DEFAULT NULL | 定时任务ID |
| `cron_task_name` | varchar(255) DEFAULT NULL | 定时任务名称 |
| `trace_id` | varchar(64) DEFAULT NULL | 关联的定时任务执行 trace_id |
| `file_url` | text NOT NULL | HTML 文件链接 |
| `file_name` | varchar(512) DEFAULT NULL | HTML 文件名 |
| `list_key` | varchar(1024) DEFAULT NULL | 名单稳定标识，默认使用文件链接 |
| `list_name` | varchar(512) DEFAULT NULL | 名单展示名称，默认使用文件名 |
| `button_id` | varchar(255) DEFAULT NULL | 按钮稳定标识 |
| `button_name` | varchar(255) DEFAULT NULL | 按钮展示名称 |
| `button_text` | varchar(512) DEFAULT NULL | 按钮文本兜底 |
| `button_type` | varchar(32) DEFAULT NULL | 按钮类型：insight/phone/other |
| `customer_id` | varchar(128) DEFAULT NULL | 客户唯一标识 |
| `customer_name` | varchar(255) DEFAULT NULL | 客户展示名称 |
| `customer_info` | text DEFAULT NULL | 点击按钮所在行的客户扩展信息 |
| `clicked_at` | datetime NOT NULL | 前端点击时间 |
| `created_at` | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | 入库时间 |
| `event_type` | varchar(32) NOT NULL DEFAULT 'button_click' | 事件类型：button_click/main_preview_view/sub_preview_view/module_exposure |
| `template_id` | bigint DEFAULT NULL | 当前事件所在模板ID |
| `result_id` | varchar(128) DEFAULT NULL | 当前模板生成结果ID |
| `event_target_id` | varchar(255) DEFAULT NULL | 模块的稳定标识 |
| `event_target_name` | varchar(512) DEFAULT NULL | 模块的展示名称 |
| `template_type` | varchar(16) DEFAULT NULL | 模板类型：main/sub |
| `page_source` | varchar(50) DEFAULT NULL | 打开 HTML 的页面来源 |
| `platform_source` | varchar(50) DEFAULT NULL | 打开 HTML 的平台来源 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `KEY `idx_clicked_at` (`clicked_at`)`
- `KEY `idx_task_clicked` (`cron_task_id`,`clicked_at`)`
- `KEY `idx_source_clicked` (`source_id`,`clicked_at`)`
- `KEY `idx_trace_id` (`trace_id`)`
- `KEY `idx_trace_clicked` (`trace_id`,`clicked_at`)`
- `KEY `idx_source_event_target_clicked` (`source_id`,`event_type`,`event_target_id`,`clicked_at`)`
- `KEY `idx_source_template_result_clicked` (`source_id`,`template_id`,`result_id`,`clicked_at`)`
- `KEY `idx_source_template_type_event_clicked` (`source_id`,`template_type`,`event_type`,`clicked_at`)`
- `KEY `idx_source_template_check` (`cron_task_id`,`template_type`,`event_type`,`event_target_id`)`
- `KEY `idx_source_template_check_button_type` (`cron_task_id`,`event_type`,`button_type`,`customer_id`)`
- `KEY `idx_source_template_check_type` (`cron_task_id`,`event_type`,`template_type`,`user_id`)`
- `KEY `idx_source_template_check_customer` (`cron_task_id`,`event_type`,`template_type`,`customer_id`)`
- `KEY `idx_source_bbk_clicked` (`source_id`,`bbk_id`,`clicked_at`)`
- `KEY `idx_source_button_event_clicked` (`source_id`,`button_type`,`event_type`,`clicked_at`)`
- `KEY `idx_source_trace_clicked` (`source_id`,`trace_id`,`clicked_at`)`

### swe_skill_contact_detail

通过 click_id 关联点击的接触明细；cust_uid 是接触客户编号，不要求等于推荐客户编号。一个点击可有多条接触流水。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `click_id` | bigint NOT NULL | 点击表主键id |
| `user_id` | varchar(50) DEFAULT NULL | 客户经理id |
| `user_name` | varchar(100) DEFAULT NULL | 客户经理姓名 |
| `bbk_id` | varchar(20) DEFAULT NULL | 分行/机构标识，预留给运营看板筛选 |
| `button_type` | varchar(32) DEFAULT NULL | 按钮类型：insight/phone/other |
| `cust_uid` | varchar(50) DEFAULT NULL | 客户唯一标识 |
| `bbk_org_id` | varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL | 二级分行号 |
| `clicked_at` | datetime NOT NULL | 前端点击时间 |
| `cont_srl_nbr` | varchar(100) NOT NULL | 接触流水号 |
| `cont_dt` | date NOT NULL | 接触日期 |
| `cont_start_tm` | datetime NOT NULL | 接触开始时间 |
| `cont_dur` | varchar(20) DEFAULT NULL | 接触时长 |
| `channel` | varchar(50) NOT NULL | 接触渠道 |
| `bal` | decimal(18,2) DEFAULT NULL | 客户时点aum |
| `skill_id` | varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL | 技能id |

DDL主键及索引：

- `PRIMARY KEY (`click_id`,`cont_srl_nbr`)`

### swe_marketplace_skills

技能目录，skill_id 是业务关联键，id 是表主键；category_id 关联分类，不能用场景产品大类代替分类。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 主键ID |
| `source_id` | varchar(64) NOT NULL | 应用入口标识 |
| `item_id` | varchar(64) NOT NULL | 市场条目ID |
| `skill_id` | varchar(128) NOT NULL | 技能唯一标识符 |
| `skill_name` | varchar(128) NOT NULL | 技能目录名 |
| `cn_name` | varchar(256) DEFAULT '' | 中文展示名 |
| `include_in_statistics` | tinyint(1) DEFAULT '1' | 是否纳入统计：1=纳入，0=不纳入 |
| `creator_id` | varchar(64) DEFAULT '' | 创建人ID |
| `creator_name` | varchar(256) DEFAULT '' | 创建人名称 |
| `updator_id` | varchar(64) DEFAULT '' | 更新人ID |
| `updator_name` | varchar(256) DEFAULT '' | 更新人名称 |
| `is_unpublished` | tinyint DEFAULT '0' | 是否下架：1=已下架，0=未下架 |
| `is_deleted` | tinyint DEFAULT '0' | 是否删除：1=已删除，0=未删除 |
| `created_at` | datetime DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `description` | text | 技能说明 |
| `version` | varchar(64) NOT NULL DEFAULT '1.0.0' | 技能版本 |
| `status` | varchar(16) NOT NULL DEFAULT 'active' | 技能状态 |
| `category_id` | bigint DEFAULT NULL | 技能分类ID |
| `bbk_ids` | text DEFAULT NULL | 适用分行号列表（JSON文本） |
| `content_path` | varchar(512) DEFAULT NULL | 技能内容路径 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_source_item` (`source_id`,`item_id`)`
- `KEY `idx_skill_id` (`skill_id`)`
- `KEY `idx_include_statistics` (`source_id`,`include_in_statistics`)`
- `KEY `idx_creator_id` (`creator_id`)`

### rm_jkh_user_info

客户经理、机构和等级信息；复合主键是分行、支行、用户，不能假定 user_id 在表结构上全局唯一。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `first_bbk_id` | varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL | 一级分行号 |
| `first_bbk_nm` | varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL | 一级分行名称 |
| `org_id` | varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL | 机构编码 |
| `org_nm` | varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL | 机构名称 |
| `first_brn_id` | varchar(20) COLLATE utf8mb4_bin DEFAULT NULL | 综合支行号 |
| `first_brn_nm` | varchar(100) COLLATE utf8mb4_bin DEFAULT NULL | 综合支行名称 |
| `user_id` | varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL | 用户ID |
| `user_name` | varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL | 用户名称 |
| `pst_lvl` | varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL | 金葵花等级L1/L2/L3 |
| `update_time` | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| `cust_cnt` | int DEFAULT NULL | 管户数量 |
| `clb_ind` | varchar(10) COLLATE utf8mb4_bin NOT NULL DEFAULT '1' | 统计类型标识 1-正常 2-当日登陆过w+ |

DDL主键及索引：

- `PRIMARY KEY (`first_bbk_id`,`org_id`,`user_id`)`
- `KEY `USER_ID_INDEX` (`user_id`)`
- `KEY `UPDATE_TIME_INDEX` (`update_time`)`
- `KEY `BBK_ID_INDEX` (`first_bbk_id`)`

### swe_marketplace_categories

技能分类字典；按 category_id=id 且技能与分类 source_id 相同关联，缺失分类名称可空。branch_visible 不作为报表过滤条件。

| 字段 | SQL定义（类型、空值、默认值） | DDL注释 |
| --- | --- | --- |
| `id` | bigint NOT NULL AUTO_INCREMENT | 分类主键ID |
| `source_id` | varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL | 应用入口标识 |
| `name` | varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL | 分类名称 |
| `sort_order` | int NOT NULL DEFAULT '0' | 排序权重，升序 |
| `created_at` | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `branch_visible` | tinyint(1) NOT NULL DEFAULT '1' | 分行是否可见 |

DDL主键及索引：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_source_name` (`source_id`,`name`)`
- `KEY `idx_source_id` (`source_id`)`

## 来源和使用边界

- [基础建表SQL](../../../database/init.sql)
- [分类建表SQL](../../../database/skill-categories.sql)
- [Mapper查询](../../../src/main/resources/mapper/report/ReportMapper.xml)
- [共享统计规则](report-rules.md)
- [模块地图](../modules/reports.md)

当前常规统计要求规划、目标、场景deleted=0，技能及任务source_id=RMASSIST，任务status不为deleted；filters采用独立范围，不继承上述所有条件。技能表删除、上下架和include_in_statistics等字段存在，不表示当前报表自动按这些字段过滤。结果与事件的枚举以Mapper和已确认口径为准，DDL注释中的历史枚举不能覆盖当前规则。
