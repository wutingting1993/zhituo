# 技能下拉按规划所属机构联动

用户批准：使用规划bbk_id、bbk_org_id筛选，规划id关联场景plan_id，scene_id关联技能skill_id；规划场景deleted=0，技能source_id=RMASSIST。未选机构直接返回全部技能。仅选分行、仅选支行或组合均支持；忽略skillIds，不依赖目标、人员、任务、执行。机构树、等级和统计口径不变。

已修改应用/support/mapper传参及XML，更新文档和旧验证脚本。指定修改Java verify退出0；全量backend-gate.sh src/main/java/退出0，静态/格式/编译/架构通过。

在18894独立端口，临时设置本任务此前已造P1机构字段，验证分行、支行、组合、错误组合及deleted=1过滤，与独立SQL取数一致；finally恢复原值。未选机构返回全部技能，机构树等级不变，skillIds不限制候选。14项隔离回归通过；其他会话造数影响的两项全行固定数断言未计入。Python语法、文档JSON及git diff --check通过。未新增索引、依赖、缓存或提交。

## 追加口径（取代前述删除过滤条件）

用户要求规划、场景不限制deleted，并要求关联任务。已改为先按规划机构读取(plan_id,scene_id)组合，再每200条关联任务及技能，任务source_id=RMASSIST、status!=deleted，plan_id和完整skill_ids必须同时匹配。未选机构仍直接全部RMASSIST技能。各SQL均为两表，不存在四表join、跨表子查询或空批查询。

新增ReportPlanSkillPo；修改mapper/support及XML。指定修改Java verify与backend-gate.sh src/main/java/均退出0；独立审阅无Important问题。18895验证：规划及场景deleted=1仍返回对应技能，任务status=deleted和任务其他来源分别排除，分行/支行/组合过滤通过，空匹配为空，skillIds不影响候选，全量无机构仍保留全部技能；临时造数字段全部恢复。14项隔离回归通过。
