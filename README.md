# zhituo

单模块 Spring Boot 2.7.2 后端，Java 8、Maven、MySQL。

## 本机启动

```bash
export JAVA_HOME=/Users/natasha/data/code/api-config/.local-run/jdk8/zulu8.98.0.107-ca-jdk8.0.512-macosx_aarch64/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -Dmaven.repo.local=.tool-cache/m2 spring-boot:run
```

默认端口 8080。数据库连接使用 MYSQL_HOST、MYSQL_PORT、MYSQL_DATABASE、MYSQL_USER、MYSQL_PASSWORD 环境变量，默认数据库名 zhituo。数据库尚未自动创建，不执行任何业务建表或写入。

组件知识与模型上下文已迁至 [component-knowledge](../component-knowledge/README.md)，独立服务默认 8081，使用原有知识表。

报表提供检索选项、顶部统计、分支行规划覆盖及规划明细接口，参数与响应见 [接口说明](docs/report-api.md)。未匹配路由通过统一异常处理器返回错误响应。

## 验证

```bash
bash scripts/java-quality.sh verify src/main/java/
bash scripts/backend-gate.sh src/main/java/
```

hook 依赖项目自身的 Git 仓库，启用方式见 scripts/HOOKS.md；需用户审查信任。

接口调用监控见 [接口说明](docs/interface-monitor-api.md)。从配置服务迁移数据库时，使用 [合库说明](database/merge-api-config.md)。
