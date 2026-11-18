# 构建与 CI 接入

将模板完整复制到项目根目录，在现有 CI 的构建步骤中执行：

```bash
bash scripts/backend-gate.sh backend/
```

参数为实际 Java 源码目录，可传多个。脚本依次执行 Checkstyle、格式一致性检查、业务项目 `mvn clean compile` 和全量编译产物架构检查；任何一步失败立即返回非零。CI 不自动格式化。首次接入需处理已有违规，禁止跳过检查掩盖问题。

本模板提供检查入口，不自动改动目标仓库 CI；仓库管理员须将该步骤配置为合并必需检查。单模块根项目可传 `src/main/java/`；根 POM 不在仓库根目录时，进入实际聚合项目目录并使用脚本绝对路径。特殊 Maven profile、代码生成、私有仓库等参数须在项目 CI 中按现有构建方式配置。

Gradle 项目分别执行其编译命令，再将实际 classes 目录传入架构脚本，不使用 Maven gate 编译业务。

架构检查命令：

```bash
python3 scripts/architecture-check.py module-a/target/classes module-b/target/classes
```

基于 ArchUnit 1.2.1 检查编译后类依赖：反向分层、应用层直连基础设施实现、Controller 绕过应用层、直接依赖服务实现、服务实现缺少接口，以及层间循环；Controller、Mapper 按名称校验归属。领域对 infrastructure.common 的公共依赖豁免，公共内容是否混入技术配置仍需审查。架构命名以规范约定为准，其他包结构先适配规则；反射调用、SQL、事务语义、业务正确性不在本检查范围内。
