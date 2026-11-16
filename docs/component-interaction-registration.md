# 组件真实交互与联动登记

日期：2026-11-16。按用户要求登记真实资料，并将「顶部指标统计」改名为「核心指标总览」、使用当前前端标识。

## 资料依据与边界

来源前端仓库 PlanReport，提交 ba3e56418ed874d63db61fee2a0a62ae12a14b58，工作区无改动。组件身份取自 components.registry.json 的 meta.id 与 title；行为正文逐项核对组件实现和页面回调，不把共同接口或组件嵌套自动当作联动。覆盖当前已实现的主要业务操作；不登记尚未实现的规划洞察能力，不为纯展示和装配组件虚构操作。

- 同一已登记页面下补充 25 个组件，共 26 个，全部 component_id 与当前前端 meta.id 完全相同（包括 DetailDrawer / ... 的完整标识）。
- 原数据库主键 328937da8dfded87 保留，component_id 从 kpiStrip 改为 MetricsStrip，名称改为核心指标总览；原有 19 条知识适用关系保持不变。知识正文中的组件ID引用同步更新，不改写原始 source。
- 页面登记路径保持原值，不将组件源码路径当成页面路径，不新增页面别名。历史验证文件中的旧标识仍作为当时结果保留。
- 筛选子控件将新值传给筛选栏，统一的报表联动由筛选栏登记；不把每个子控件到全部报表区块的间接影响重复存储。父级装配组件不重复登记子组件操作。
- 规划表与执行表的导出使用页面共享结果，不登记表格向导出按钮供数的虚假关系。
- 抽屉的筛选联动仅在对应抽屉已打开且页面数据可用时成立。无当前实例时不声称执行请求。
- 纯展示组件可以只有依赖来源而没有用户操作，例如核心指标总览。具体抽屉的关闭操作来自共用 DrawerFrame 和页面 closeDrawer，按当前抽屉实例登记，不虚构跨组件关系。

## 执行方式

新增主键由当前项目的 CUuid.nextId() 实际生成，共使用 120 个（25 组件、39 交互、56 关系）。不使用 UUID、随机拼接或数据库自动生成ID。执行前核实本机 zhituo 库仅有原组件、两张新表为空，并核对目标页面系统项目。

```sh
python3 /private/tmp/zhituo-register-real-interactions.py
```

该执行器使用 application.yml 的数据库配置，执行 database/component-interaction-data.sql，在一个事务中写入并提交；未打印凭据。数据脚本是一次性登记，重复执行主键冲突时应停止，不使用 --force。新库按 knowledge-schema.sql、knowledge-data.sql、component-interaction-data.sql 顺序执行；已登记当前资料的库不重复执行。

同步 knowledge-data.sql、report-summary-knowledge-data.sql、page-component-description.sql 和 structured-interface-context-data.sql 中相关组件标识，防止重新初始化或维护时恢复旧标识、旧名称。现有接口自身的登记名称不在组件重命名范围内。

## 组件清单

| 前端标识 / 后端 component_id | 中文名称 | 数据库主键 |
| --- | --- | --- |
| DrawerFrame | 抽屉外壳 | 328da30d6b03ed87 |
| DetailDrawer / Glossary | 指标口径抽屉 | 328da30d6b04ed87 |
| DetailDrawer / ManagersDrawer | 支行客户经理明细抽屉 | 328da30d6b05ed87 |
| DetailDrawer / PlansDrawer | 规划明细抽屉 | 328da30d6b06ed87 |
| DetailDrawer / SkillsDrawer | 支行技能明细抽屉 | 328da30d6b07ed87 |
| DetailDrawer | 明细抽屉 | 328da30d6b08ed87 |
| BranchWatch | 需关注支行 | 328da30d6b09ed87 |
| ExecutionFunnel | 执行链路漏斗 | 328da30d6b0aed87 |
| ExecutionTable | 支行执行明细表 | 328da30d6b0bed87 |
| SkillRanking | 技能参与度排行 | 328da30d6b0ced87 |
| TrendChart | 执行走势图 | 328da30d6b0ded87 |
| ExecutionView | 执行视角 | 328da30d6b0eed87 |
| DateFilter | 统计日期筛选 | 328da30d6b0fed87 |
| ManagerLevelFilter | 客户经理等级筛选 | 328da30d6b10ed87 |
| OrganizationFilter | 分行/支行筛选 | 328da30d6b11ed87 |
| SkillFilter | 技能筛选 | 328da30d6b12ed87 |
| FiltersBar | 筛选栏 | 328da30d6b13ed87 |
| MetricsStrip | 核心指标总览 | 328937da8dfded87 |
| PlanCoverageTable | 分支行规划覆盖表 | 328da30d6b14ed87 |
| PlanInsights | 规划洞察 | 328da30d6b15ed87 |
| PlanSourceBreakdown | 规划来源结构 | 328da30d6b16ed87 |
| PlanningView | 规划视角 | 328da30d6b17ed87 |
| ReportHeader | 报表标题与操作 | 328da30d6b18ed87 |
| Topbar | 顶部导航栏 | 328da30d6b19ed87 |
| ViewSwitcher | 视角切换 | 328da30d6b1aed87 |
| Layout | 页面主体区域 | 328da30d6b1bed87 |

## 交互与证据

以下是实际写入的操作，源码路径均相对于上述前端仓库。条件、传递内容及自身响应完整保存在 SQL 中。

| 交互主键 | 所属组件 | 操作 | 触发条件 | 实现依据 |
| --- | --- | --- | --- | --- |
| 328da30d6b1ced87 | FiltersBar | 应用筛选条件 | 子筛选控件回传有效的新条件。 | src/pages/PlanReport/components/FiltersBar/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b1ded87 | FiltersBar | 刷新报表 | 用户点击筛选栏的刷新按钮。 | src/pages/PlanReport/components/FiltersBar/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b1eed87 | FiltersBar | 重置筛选 | 用户点击重置筛选。 | src/pages/PlanReport/components/FiltersBar/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b1fed87 | FiltersBar | 关闭筛选浮层 | 日期或技能浮层已打开，用户点击浮层外部或按 Esc。 | src/pages/PlanReport/components/FiltersBar/index.tsx |
| 328da30d6b20ed87 | DateFilter | 选择快捷日期 | 用户点击今日、近7天或近30天。 | src/pages/PlanReport/components/FiltersBar/DateFilter/index.tsx |
| 328da30d6b21ed87 | DateFilter | 应用自定义日期 | 起止日期完整且区间不超过一个月，用户点击确定。 | src/pages/PlanReport/components/FiltersBar/DateFilter/index.tsx |
| 328da30d6b22ed87 | DateFilter | 展开或收起日期浮层 | 用户点击日期范围按钮。 | src/pages/PlanReport/components/FiltersBar/DateFilter/index.tsx |
| 328da30d6b23ed87 | DateFilter | 取消日期编辑 | 日期浮层已打开，用户点击取消。 | src/pages/PlanReport/components/FiltersBar/DateFilter/index.tsx |
| 328da30d6b24ed87 | DateFilter | 提示日期校验问题 | 起止日期不完整或区间超过一个月时点击确定。 | src/pages/PlanReport/components/FiltersBar/DateFilter/index.tsx |
| 328da30d6b25ed87 | OrganizationFilter | 搜索机构 | 用户在分行或支行下拉框输入名称。 | src/pages/PlanReport/components/FiltersBar/OrganizationFilter/index.tsx |
| 328da30d6b26ed87 | OrganizationFilter | 切换分行 | 用户选择具体分行或全部分行。 | src/pages/PlanReport/components/FiltersBar/OrganizationFilter/index.tsx |
| 328da30d6b27ed87 | OrganizationFilter | 选择具体支行 | 选中的支行存在于当前候选列表中。 | src/pages/PlanReport/components/FiltersBar/OrganizationFilter/index.tsx |
| 328da30d6b28ed87 | OrganizationFilter | 选择全部支行 | 用户选择全部支行。 | src/pages/PlanReport/components/FiltersBar/OrganizationFilter/index.tsx |
| 328da30d6b29ed87 | ManagerLevelFilter | 选择客户经理等级 | 用户选择具体等级或全部等级。 | src/pages/PlanReport/components/FiltersBar/ManagerLevelFilter/index.tsx |
| 328da30d6b2aed87 | SkillFilter | 展开或收起技能浮层 | 用户点击技能按钮。 | src/pages/PlanReport/components/FiltersBar/SkillFilter/index.tsx |
| 328da30d6b2bed87 | SkillFilter | 勾选或取消技能 | 用户切换技能复选框。 | src/pages/PlanReport/components/FiltersBar/SkillFilter/index.tsx |
| 328da30d6b2ced87 | SkillFilter | 清空技能 | 用户点击技能浮层中的清空。 | src/pages/PlanReport/components/FiltersBar/SkillFilter/index.tsx |
| 328da30d6b2ded87 | ViewSwitcher | 切换到规划视角 | 用户点击规划视角按钮。 | src/pages/PlanReport/components/ViewSwitcher/index.tsx<br>src/pages/PlanReport/index.tsx<br>src/pages/PlanReport/components/PlanningView/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx |
| 328da30d6b2eed87 | ViewSwitcher | 切换到执行视角 | 用户点击执行视角按钮。 | src/pages/PlanReport/components/ViewSwitcher/index.tsx<br>src/pages/PlanReport/index.tsx<br>src/pages/PlanReport/components/PlanningView/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx |
| 328da30d6b2fed87 | PlanCoverageTable | 展开或收起分行 | 分行编号有效，用户点击展开按钮。 | src/pages/PlanReport/components/PlanningView/PlanCoverageTable/index.tsx |
| 328da30d6b30ed87 | PlanCoverageTable | 查看分行规划明细 | 分行编号有效、存在下属支行且规划数大于0，用户点击可下钻数量。 | src/pages/PlanReport/components/PlanningView/PlanCoverageTable/index.tsx<br>src/pages/PlanReport/components/PlanningView/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b31ed87 | PlanCoverageTable | 查看支行规划明细 | 支行与所属分行编号有效且规划数大于0，用户点击可下钻数量。 | src/pages/PlanReport/components/PlanningView/PlanCoverageTable/index.tsx<br>src/pages/PlanReport/components/PlanningView/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b32ed87 | PlanCoverageTable | 空态重置筛选 | 表格暂无数据，用户点击重置筛选。 | src/pages/PlanReport/components/PlanningView/PlanCoverageTable/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b33ed87 | ExecutionTable | 切换排序 | 用户点击可排序表头。 | src/pages/PlanReport/components/ExecutionView/ExecutionTable/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b34ed87 | ExecutionTable | 查看支行客户经理明细 | 当前行分行和支行标识均存在，用户点击可下钻的支行名称。 | src/pages/PlanReport/components/ExecutionView/ExecutionTable/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b35ed87 | ExecutionTable | 查看支行技能明细 | 当前行分行和支行标识均存在，用户点击可下钻的技能数。 | src/pages/PlanReport/components/ExecutionView/ExecutionTable/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b36ed87 | ExecutionTable | 空态重置筛选 | 表格暂无数据，用户点击重置筛选。 | src/pages/PlanReport/components/ExecutionView/ExecutionTable/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b37ed87 | BranchWatch | 查看支行客户经理明细 | 关注列表行的分行与支行标识存在，用户点击可下钻支行名称。 | src/pages/PlanReport/components/ExecutionView/BranchWatch/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b38ed87 | Topbar | 查看指标口径 | 用户点击指标口径按钮。 | src/pages/PlanReport/components/Topbar/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b39ed87 | ReportHeader | 查看指标口径 | 用户点击指标口径按钮。 | src/pages/PlanReport/components/ReportHeader/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b3aed87 | ReportHeader | 导出当前视角 | 报表已加载且导出按钮可用，用户点击导出CSV。 | src/pages/PlanReport/components/ReportHeader/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b3bed87 | TrendChart | 切换曲线模式 | 执行视角下用户点击转化率或客户量。 | src/pages/PlanReport/components/ExecutionView/TrendChart/index.tsx<br>src/pages/PlanReport/components/ExecutionView/index.tsx |
| 328da30d6b3ced87 | TrendChart | 查看日期数据提示 | 用户将鼠标移到图中的日期位置。 | src/pages/PlanReport/components/ExecutionView/TrendChart/index.tsx |
| 328da30d6b3ded87 | SkillRanking | 查看技能参与提示 | 用户悬停技能条形。 | src/pages/PlanReport/components/ExecutionView/SkillRanking/index.tsx |
| 328da30d6b3eed87 | DetailDrawer / PlansDrawer | 切换规划来源 | 用户点击全部或某一来源标签。 | src/pages/PlanReport/components/DetailDrawer/PlansDrawer/index.tsx |
| 328da30d6b3fed87 | DetailDrawer / PlansDrawer | 关闭抽屉 | 当前抽屉已打开，用户点击关闭或遮罩，或在抽屉内按Esc。 | src/pages/PlanReport/components/DetailDrawer/PlansDrawer/index.tsx<br>src/pages/PlanReport/components/DetailDrawer/DrawerFrame/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b40ed87 | DetailDrawer / ManagersDrawer | 关闭抽屉 | 当前抽屉已打开，用户点击关闭或遮罩，或在抽屉内按Esc。 | src/pages/PlanReport/components/DetailDrawer/ManagersDrawer/index.tsx<br>src/pages/PlanReport/components/DetailDrawer/DrawerFrame/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b41ed87 | DetailDrawer / SkillsDrawer | 关闭抽屉 | 当前抽屉已打开，用户点击关闭或遮罩，或在抽屉内按Esc。 | src/pages/PlanReport/components/DetailDrawer/SkillsDrawer/index.tsx<br>src/pages/PlanReport/components/DetailDrawer/DrawerFrame/index.tsx<br>src/pages/PlanReport/index.tsx |
| 328da30d6b42ed87 | DetailDrawer / Glossary | 关闭抽屉 | 当前抽屉已打开，用户点击关闭或遮罩，或在抽屉内按Esc。 | src/pages/PlanReport/components/DetailDrawer/Glossary/index.tsx<br>src/pages/PlanReport/components/DetailDrawer/DrawerFrame/index.tsx<br>src/pages/PlanReport/index.tsx |

## 联动清单

| 关系主键 | 来源组件 / 操作 | 目标组件 | 对目标的影响 | 生效条件 |
| --- | --- | --- | --- | --- |
| 328da30d6b43ed87 | FiltersBar / 应用筛选条件 | MetricsStrip | 按当前筛选范围重新查询并更新六项汇总指标；当日或周期任务数标题随统计天数变化。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b44ed87 | FiltersBar / 应用筛选条件 | PlanCoverageTable | 按当前筛选范围更新分行及支行的规划覆盖结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b45ed87 | FiltersBar / 应用筛选条件 | PlanSourceBreakdown | 按当前筛选范围更新规划来源结构和分行排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b46ed87 | FiltersBar / 应用筛选条件 | ExecutionFunnel | 按当前筛选范围更新执行链路的客户数量与转化结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b47ed87 | FiltersBar / 应用筛选条件 | TrendChart | 按当前筛选范围更新逐日执行走势。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b48ed87 | FiltersBar / 应用筛选条件 | ExecutionTable | 按当前筛选范围及当前排序更新支行执行明细和机构基准。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b49ed87 | FiltersBar / 应用筛选条件 | SkillRanking | 按当前筛选范围更新技能参与度排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b4aed87 | FiltersBar / 应用筛选条件 | BranchWatch | 按当前筛选范围更新需关注支行列表。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b4bed87 | FiltersBar / 应用筛选条件 | DetailDrawer / PlansDrawer | 沿用已选下钻机构和当前筛选范围更新规划明细。 | 规划明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b4ced87 | FiltersBar / 应用筛选条件 | DetailDrawer / ManagersDrawer | 沿用已选支行及所属分行，按当前筛选范围更新客户经理执行明细。 | 客户经理明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b4ded87 | FiltersBar / 应用筛选条件 | DetailDrawer / SkillsDrawer | 沿用已选支行及所属分行，按当前筛选范围更新技能执行明细。 | 技能明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b4eed87 | FiltersBar / 刷新报表 | MetricsStrip | 按当前筛选范围重新查询并更新六项汇总指标；当日或周期任务数标题随统计天数变化。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b4fed87 | FiltersBar / 刷新报表 | PlanCoverageTable | 按当前筛选范围更新分行及支行的规划覆盖结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b50ed87 | FiltersBar / 刷新报表 | PlanSourceBreakdown | 按当前筛选范围更新规划来源结构和分行排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b51ed87 | FiltersBar / 刷新报表 | ExecutionFunnel | 按当前筛选范围更新执行链路的客户数量与转化结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b52ed87 | FiltersBar / 刷新报表 | TrendChart | 按当前筛选范围更新逐日执行走势。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b53ed87 | FiltersBar / 刷新报表 | ExecutionTable | 按当前筛选范围及当前排序更新支行执行明细和机构基准。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b54ed87 | FiltersBar / 刷新报表 | SkillRanking | 按当前筛选范围更新技能参与度排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b55ed87 | FiltersBar / 刷新报表 | BranchWatch | 按当前筛选范围更新需关注支行列表。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b56ed87 | FiltersBar / 刷新报表 | DetailDrawer / PlansDrawer | 沿用已选下钻机构和当前筛选范围更新规划明细。 | 规划明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b57ed87 | FiltersBar / 刷新报表 | DetailDrawer / ManagersDrawer | 沿用已选支行及所属分行，按当前筛选范围更新客户经理执行明细。 | 客户经理明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b58ed87 | FiltersBar / 刷新报表 | DetailDrawer / SkillsDrawer | 沿用已选支行及所属分行，按当前筛选范围更新技能执行明细。 | 技能明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b59ed87 | FiltersBar / 重置筛选 | MetricsStrip | 按当前筛选范围重新查询并更新六项汇总指标；当日或周期任务数标题随统计天数变化。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5aed87 | FiltersBar / 重置筛选 | PlanCoverageTable | 按当前筛选范围更新分行及支行的规划覆盖结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5bed87 | FiltersBar / 重置筛选 | PlanSourceBreakdown | 按当前筛选范围更新规划来源结构和分行排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5ced87 | FiltersBar / 重置筛选 | ExecutionFunnel | 按当前筛选范围更新执行链路的客户数量与转化结果。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5ded87 | FiltersBar / 重置筛选 | TrendChart | 按当前筛选范围更新逐日执行走势。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5eed87 | FiltersBar / 重置筛选 | ExecutionTable | 按当前筛选范围及当前排序更新支行执行明细和机构基准。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b5fed87 | FiltersBar / 重置筛选 | SkillRanking | 按当前筛选范围更新技能参与度排名。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b60ed87 | FiltersBar / 重置筛选 | BranchWatch | 按当前筛选范围更新需关注支行列表。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b61ed87 | FiltersBar / 重置筛选 | DetailDrawer / PlansDrawer | 沿用已选下钻机构和当前筛选范围更新规划明细。 | 规划明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b62ed87 | FiltersBar / 重置筛选 | DetailDrawer / ManagersDrawer | 沿用已选支行及所属分行，按当前筛选范围更新客户经理执行明细。 | 客户经理明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b63ed87 | FiltersBar / 重置筛选 | DetailDrawer / SkillsDrawer | 沿用已选支行及所属分行，按当前筛选范围更新技能执行明细。 | 技能明细抽屉当前已打开，且页面报表数据可用。 |
| 328da30d6b64ed87 | DateFilter / 选择快捷日期 | FiltersBar | 用新日期替换生效条件中的统计日期。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b65ed87 | DateFilter / 应用自定义日期 | FiltersBar | 将统计日期更新为已应用的自定义区间。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b66ed87 | OrganizationFilter / 切换分行 | FiltersBar | 同时更新生效条件中的分行与支行。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b67ed87 | OrganizationFilter / 选择具体支行 | FiltersBar | 用成对的分行和支行标识更新机构条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b68ed87 | OrganizationFilter / 选择全部支行 | FiltersBar | 清除支行限制并保留分行条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b69ed87 | ManagerLevelFilter / 选择客户经理等级 | FiltersBar | 更新生效条件中的客户经理等级。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6aed87 | SkillFilter / 勾选或取消技能 | FiltersBar | 用新的技能集合更新筛选条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6bed87 | SkillFilter / 清空技能 | FiltersBar | 清除技能筛选限制。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6ced87 | ViewSwitcher / 切换到规划视角 | PlanningView | 显示规划视角内容。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6ded87 | ViewSwitcher / 切换到规划视角 | ExecutionView | 隐藏另一视角内容。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6eed87 | ViewSwitcher / 切换到规划视角 | ReportHeader | 后续导出使用当前规划视角对应的数据。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b6fed87 | ViewSwitcher / 切换到执行视角 | ExecutionView | 显示执行视角内容。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b70ed87 | ViewSwitcher / 切换到执行视角 | PlanningView | 隐藏另一视角内容。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b71ed87 | ViewSwitcher / 切换到执行视角 | ReportHeader | 后续导出使用当前执行视角对应的数据。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b72ed87 | PlanCoverageTable / 查看分行规划明细 | DetailDrawer / PlansDrawer | 打开规划明细抽屉，查询并展示所选分行的规划明细。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b73ed87 | PlanCoverageTable / 查看支行规划明细 | DetailDrawer / PlansDrawer | 打开规划明细抽屉，查询并展示所选支行的规划明细。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b74ed87 | PlanCoverageTable / 空态重置筛选 | FiltersBar | 恢复并显示默认筛选条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b75ed87 | ExecutionTable / 查看支行客户经理明细 | DetailDrawer / ManagersDrawer | 打开客户经理明细抽屉，沿用当前筛选范围，展示被点击支行的客户经理执行情况。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b76ed87 | ExecutionTable / 查看支行技能明细 | DetailDrawer / SkillsDrawer | 打开技能明细抽屉，沿用当前筛选范围，展示被点击支行的技能执行情况。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b77ed87 | ExecutionTable / 空态重置筛选 | FiltersBar | 恢复并显示默认筛选条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b78ed87 | BranchWatch / 查看支行客户经理明细 | DetailDrawer / ManagersDrawer | 打开该支行的客户经理执行明细，并沿用当前筛选条件。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b79ed87 | Topbar / 查看指标口径 | DetailDrawer / Glossary | 打开指标口径说明抽屉。 | 未单独登记条件，结合来源操作阅读 |
| 328da30d6b7aed87 | ReportHeader / 查看指标口径 | DetailDrawer / Glossary | 打开指标口径说明抽屉。 | 未单独登记条件，结合来源操作阅读 |

## 真实 HTTP 验证

只读调用最终后端实现的 POST /api/v1/knowledge/component-context，临时服务监听 127.0.0.1:18106。验证脚本：

```sh
python3 /private/tmp/verify-zhituo-real-interactions.py
```

- 26 个组件均命中正确数据库主键、前端标识及中文名称。
- 39 条交互及每个组件的交互顺序与登记 SQL 逐项一致。
- 56 条联动逐条核对：来源 outputs 与目标 inputs 的关系ID、交互ID、两端身份、正文、条件、传递内容和响应完全一致；所有 inputs 合计56条，outputs合计56条。
- 筛选栏、核心指标总览、执行表、客户经理明细重复查询顺序稳定。
- kpiStrip 不再命中组件，返回 COMPONENT_NOT_FOUND 和空交互/联动；没有旧标识别名。
- 另一未登记项目不返回本项目组件及联动。
- 本轮仅数据、SQL 和文档改动，未修改 Java 或前端；未运行额外的业务单元/e2e 测试。数据库真实资料的查询验证已获用户明确授权。

具体计数见 knowledge/validation/component-interaction-real-data.json。真实资料不等于已完成大规模容量验证；仅核对当前数据及其查询计划，不宣称生产性能达标。


## 数据完整性及查询计划

真实登记后检查：孤立交互0条，无效关系（含孤立引用、跨系统项目、自关联或空白影响正文）0条。筛选栏、核心指标总览及执行表两条查询的 EXPLAIN 已实际执行，候选关系分别使用 idx_interaction_component、idx_relation_interaction、idx_relation_target；优化器仍按小表成本选择部分 ALL 访问并使用临时排序，不能据此声称大规模数据性能达标。完整输出见 knowledge/validation/component-interaction-explain.txt。数据库写入核对见 knowledge/validation/component-interaction-database-write.json。
