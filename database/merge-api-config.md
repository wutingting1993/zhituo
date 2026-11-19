# 将配置服务数据库合入 zhituo

目标为已初始化的 `zhituo` MySQL 库。保留智拓现有表，将配置库的所有普通表以及指定源库的 `operation_task` 复制到目标。脚本保留源表定义和主键值。ClickHouse 是监控统计数据源，使用独立的既有接口。

## 准备

1. 备份配置库、专家源库和目标库，确认有可用恢复点。准备 MySQL 8 的 `mysql` 和 `mysqldump` 客户端。
2. 为两个源库和目标库分别准备权限受限的 MySQL 客户端配置文件，建议权限 `0600`。每个文件的 `[client]` 段配置 `host`、`port`、`user`、`password`。配置库和专家表同源时两个参数可使用同一文件和库名。
3. 在维护窗口暂停所有源表写入。目标服务在校验完成后开始写入迁移表。

## 执行

```sh
bash database/merge-api-config.sh schema ./config.cnf api_config ./expert.cnf SOURCE_EXPERT_SCHEMA ./target.cnf
bash database/merge-api-config.sh data ./config.cnf api_config ./expert.cnf SOURCE_EXPERT_SCHEMA ./target.cnf
```

将 `SOURCE_EXPERT_SCHEMA` 换成 `operation_task` 的实际源库名。建表阶段遇到目标已存在的同名表会停止。数据阶段要求目标表存在且为空，逐表校验迁移前后行数。DDL 在 MySQL 中会隐式提交，如果建表中途失败，按备份和实际执行结果检查已落表项后再处理。源表数据保留在原库。

按 [环境变量示例](shared-database.env.example) 将智拓服务、接口配置服务和接口执行服务设为同一个 MySQL 实例及 `MYSQL_DATABASE=zhituo`。如果专家数据由独立服务维护，同一切换窗口将该写入方指向合并后的 `operation_task`，确保后续改名能在监控页显示。各服务沿用自己的部署账号和凭据。

切换后先验证接口配置查询和批量执行，再验证智拓的接口视图、专家视图、统计完成时间和手动更新。开始在目标库写入前可把部署连接改回原库。目标库开始产生新数据后，如需回切，先核对切换后产生的数据。
