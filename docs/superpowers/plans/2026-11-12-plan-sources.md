# 规划来源 Top 5 Implementation Plan

**Goal:** 实施已确认的规划来源结构接口。
**Architecture:** 在现有规划覆盖应用服务中复用加载、来源筛选及分行去重口径，新增精简来源 DTO 和 Controller 路由。
**Tech Stack:** Java 8、Spring Boot、Lombok、Guava。
**Spec:** docs/report-plan-source-requirements.md

## 约束与检查重点

仅修改规划来源相关文件；不增加依赖、SQL、鉴权、洞察或页面。不新增或运行未经授权的测试。先筛选后排名，跨支行去重，并列按分行编号升序；最多5项，无数据空列表，响应不带支行明细。

## 实施步骤

- [x] 新增 PlanSourceRow（id、name、planCount、myCount、branchCount、presidentCount）及 PlanSourcesResponse（branches 快照）。
- [x] PlanCoverageApplicationService 新增 sources(ReportSummaryRequest)，实现复用 load/plans/included/sourceCount，分行聚合后排序 limit(5)。
- [x] ReportController 新增 POST /plan-sources，沿用 @Valid 和 Result。
- [x] 对修改的 Java 文件执行 java-quality verify、编译和架构检查；检查需求覆盖与引用。
