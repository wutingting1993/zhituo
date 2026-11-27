# 共用监控汇总区实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实施用户已确认的批量请求主指标与内部接口辅助栏。
**Architecture:** 新增页面专用 MonitorSummary 组件，复用 useBatchMetrics；页面原 overview 继续提供辅助栏、图表和 Top 10。筛选组件及后端保持现状。
**Tech Stack:** 现有 React 18、TypeScript、Ant Design、Less。
**Spec:** ../specs/2026-11-27-shared-monitor-summary-design.md

## Global Constraints
- 不引入依赖，不改变筛选和图表统计规则，不提交无关改动。
- 仅执行必要静态检查、构建和已授权本机页面验证，不新增测试。

## Review Focus
- 无覆盖与零事件的显示不同；成功率排除取消。
- 接口筛选改变内部指标，但不触发批量指标重算。
- 加载和错误独立，保留内部图表。
- 1440/1920 宽度下值、单位、tooltip 和长名称不裁切。

## Task 1：汇总组件与页面接入
- [x] 新增 MonitorSummary/index.tsx、index.less、component.meta.ts，四组主指标和紧凑辅助栏，复用已有指标 tooltip。
- [x] 页面替换原指标条，保持筛选、图表及视图切换；三个排行榜移入对应视图的表格上方；给图表和排行榜增加口径 tooltip。
- [x] 更新 README、生成 InterfaceMonitor 注册表，执行 format:check、typecheck、lint、meta:check、build。
- [x] 本机浏览器核对 1440/1920、视图切换、筛选影响、tooltip 和数据一致性。


## 验证记录
- 前端 format:check、typecheck、lint、meta:check、build 均通过，构建仅有既有包体积和 ECharts 导入方式告警。
- 浏览器 1440×960、1920×1080：批量主指标、辅助栏无裁切；共用区切换视图后不变；统计时间各仅一处。
- 接口筛选后批量请求数保持 300，内部执行数由 1791 变为 697；仅取消专家显示 30 次请求、0 次完成，成功率与耗时不可用，成功 TPS 为 0。
- 接口视图两榜、专家视图一榜，均位于 Tabs 下方和对应表格上方；请求结果分布与调用趋势移入 Tabs 下方、Top 10 上方。
- 口径 tooltip 已核对鼠标/键盘聚焦；控制台仅有既有 workbench 资源 404。
- 按请求代码审查技能完成独立只读审查，未发现需要修正的问题。其后 Top N 位置调整已另行通过上述 DOM 顺序与双视口浏览器核对。
- 本次无需数据库迁移或后端重启；未新增依赖、测试、接口或提交代码。

最终补充验证：请求结果分布与调用趋势移入视图后，1440/1920 两档、接口/专家两种视图均通过 DOM 位置核对，顺序为共用汇总、Tabs、图表、Top 10、表格，统计时间仍各一处。截图位于 /tmp/shared-view-final-interface-1440.png 等四张。
