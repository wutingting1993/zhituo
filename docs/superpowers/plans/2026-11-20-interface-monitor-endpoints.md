# 接口监控查询拆分实施计划

> 在当前会话执行时使用 superpowers:executing-plans。

**目标** 同步切换智拓后端和报表前端的监控查询，删除旧的通用入口。

**设计** [接口监控查询拆分](../specs/2026-11-20-interface-monitor-endpoints-design.md)

**技术栈** Spring Boot、Java、React、TypeScript。沿用两项目的现有构建与检查命令。

## 1. 后端查询契约

- [ ] 修改 `InterfaceMonitorController`、`MonitorApplicationService`、`MonitorQuery` 和 `InterfaceMonitorService`。在监控前缀下声明五个 POST 方法 `overview`、`interfaces`、`experts`、`summary`、`trend`，删除通用 `query` 路由。请求仅包含日期与业务筛选，继续用现有响应信封。
- [ ] 在服务内固定统计口径。总览使用无筛选总计和榜单，接口列表按接口分组，专家列表按专家分组，详情汇总按所选范围总计。详情趋势按单日 600 秒、多日 86400 秒分桶，保留离线覆盖信息。复用现有统计和结果组装，沿用已配置的 CK 路径、日期校验、未归属筛选及 Mock 实现。
- [ ] 编译并运行改动文件的 Java 质量检查和后端架构检查。使用本地 Mock 核对五个 POST 入口、无记录的响应、单日与多日趋势、按接口或专家筛选和旧路径已移除。项目单元测试另行授权。

## 2. 前端调用切换

- [ ] 修改 `src/pages/InterfaceMonitor/data/api.ts`、`data/useMonitorQuery.ts` 和 `types.ts`。登记五个新 endpoint，让查询 hook 接收明确的查询用途。从 HTTP 请求体移除 `groupBy`、`bucketSeconds`，保留请求取消、并发和响应校验逻辑。
- [ ] 在 `index.tsx` 切换首页总览、接口列表和专家列表。在 `components/MonitorDetail/index.tsx` 切换详情汇总、趋势和专家列表。在 `components/ExpertDrawer/index.tsx` 切换专家汇总和接口列表，保留该详情现有的趋势装配。核对 `route.ts` 的接口登记、受影响组件清单、生成的注册表和监控页 README。
- [ ] 执行前端类型、Lint、清单及构建检查。使用本地 Mock 手动核对首页和两个详情视图，包括加载、空态、筛选、趋势与离线刷新。核对网络请求使用五个新路径且不发送旧分组字段。自动化测试另行授权。
