# 接口监控 Java 类型实施计划

> 在当前会话执行时使用 superpowers:executing-plans。

**目标** 将智拓后端监控模块的 JSON 树替换为明确的 Java 类型，保持五个查询接口的 JSON 契约。

**设计** [接口监控 Java 类型设计](../specs/2026-11-20-interface-monitor-typed-models-design.md)

**技术栈** Java、Spring Boot、RestTemplate、Jackson、Lombok。

## 1. 类型化数据访问

- [ ] 在领域监控模型中定义查询范围、统计筛选、统计行、覆盖记录及刷新命令。修改 `MonitorDataSupport`，查询返回带元素类型的 `List`，刷新返回 `void`。
- [ ] 修改 `MonitorDataSupportImpl`，为 CK 请求参数和响应信封声明协议对象。用 RestTemplate 反序列化带具体元素类型的响应，在适配边界转换成领域对象。沿用三个配置路径、超时和错误码，保留 CK 数值既可能是 JSON 数字也可能是数字字符串的支持。
- [ ] 修改 `MonitorMockData`，使用同一组领域类型和原有数据生成逻辑，保留按日期、分组和专家筛选的行为。

## 2. 类型化应用输出

- [ ] 在应用 DTO 中定义查询结果、列表项、指标、覆盖记录、总览和榜单对象。用明确的 Jackson 字段名保持现有命名，包括外层 camelCase、指标和覆盖字段 snake_case。保留缺失比率为 null、计数补零、空列表和仅总览包含 overview 的行为。
- [ ] 修改 `InterfaceMonitorService`，使用访问器归并两个监控口径、补齐空桶、关联名称和排序。保留原有 Top 10 的过滤和同分排序。
- [ ] 修改 `MonitorApplicationService` 和 `InterfaceMonitorController` 的返回类型。五个查询沿用现有路径和响应信封，刷新成功返回 `Result<Void>`。

## 3. 验证

- [ ] 对改动 Java 文件执行 `scripts/java-quality.sh verify`，执行编译、架构检查及 `git diff --check`。
- [ ] 使用本地 Mock 通过前端代理核对五个查询及刷新，覆盖空结果、可空指标、Top 10、已覆盖的空桶和错误信封。确认监控模块没有残留 JSON 树用法。项目单元测试和 E2E 测试另行授权，真实 CK 待目标环境联调。
