# 顶部统计 SQL 聚合功能验证

2026-11-12，本机现有MySQL zhituo验证数据，未插入、修改或删除数据库记录。未执行性能测试。

## 实现范围

仅将POST /api/v1/reports/summary切换到Mapper大SQL聚合，显式INNER JOIN、LEFT JOIN与非递归CTE组织关系、排程、选定执行、结果及接触统计。查询只返回一行聚合记录，Java不再加载该接口的客户明细，仍计算比率、差值并组装响应。其他接口仍使用原路径。

客户标识及相关编号聚合显式采用二进制语义，避免数据库默认不区分大小写排序规则改变Java集合去重语义。执行时间相同按主键倒序任取一条；同技能多个名称取最小名称，原先无排序的首条名称不稳定。该显示边界已同步接口说明。

## 对照方法与结果

- before：/private/tmp/zhituo-summary-baseline，复制本轮源码后，仅将summary计算恢复为原执行、推荐和接触明细路径；不是Git历史版本。
- after：当前项目源码，使用最终SQL版本。
- 临时端口分别18101、18102，连接同一本机数据库；未替换已有服务。
- [脚本](../scripts/summary-sql-validation/verify.py)比较HTTP状态和完整JSON，不忽略字段、数组顺序、空值或错误消息；[结果](../scripts/summary-sql-validation/results.json)保存最终响应。

实际命令：`python3 scripts/summary-sql-validation/verify.py`。最终版本51/51通过：37项summary对照（31项正常请求、6项日期参数错误）与14项其他接口回归。

覆盖两套既有验证数据的全行、分行、支行、等级、单/多技能、组合条件、区间首尾日、空日期、未知技能、机构不匹配、空/null技能数组，以及NULL链路、无执行和205名客户经理跨批次。额外断言应执行=已执行+待执行、空范围零值及null比率、主分行非空结果，避免只验证两边都返回空。

## 静态及构建检查

- `bash scripts/java-quality.sh verify`指定本次6个Java文件：ReportSummaryPo、ReportMapper、ReportDataSupport、ReportDataSupportImpl、ReportDataConverter、ReportApplicationServiceImpl，退出码0。
- `bash scripts/backend-gate.sh src/main/java/`：89个Java文件，静态/格式、编译及架构检查通过，退出码0；架构检查1项，失败0、错误0。
- Mapper XML由ElementTree解析通过；验证脚本由ast.parse检查通过。项目未配置Python lint，语法检查不等于完整Python静态检查。
- 初次Java检查发现新增方法放在实例属性前，已调整声明顺序并重新检查通过。初次回归脚本未嵌套skill-participation的filters，已修正请求后全套通过；最终结果文件不包含该失败轮次。

## 限制

本轮证明当前现有验证数据下新旧响应一致，不代表生产规模性能、任意数据及并发稳定性。未新增大小写客户UID、同技能多名称、同时间并列执行等专项造数；这些边界完成代码审阅但不宣称已运行覆盖。没有执行执行计划分析、压力测试或量化提速。临时服务验证后停止，源码对照副本保留，复验须重新启动两个服务。
