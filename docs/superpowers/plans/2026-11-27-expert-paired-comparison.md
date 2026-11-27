# 专家分组双行对比表实施计划

用户已选择并同意「专家分组双行」草图，在当前会话直接实施。

- [x] 专家名称跨两行；接口/上游共用六指标列；保留占比分母、空值、名称省略和完整 tooltip。
- [x] 按所选接口/上游口径对专家分组排序，升序、降序、取消均保持两行相邻，缺失值置底。
- [x] 调用次数对比条、固定口径颜色、分组底色与排序列提示沿用草图；不改变页面筛选。
- [x] 同步 metadata、README，执行 format/typecheck/lint/meta/build/diff 检查。
- [x] 1440/1920 实测宽度、排序与分组稳定、口径切换和长名称；确认六卡片与其他表格不受影响。

用户明确同意一并修正数据范围：新增 POST /expert-executions，复用既有聚合方法并强制 interfaceId，主 /experts 继续忽略接口筛选。

验证记录：前端 format:check/typecheck/lint/meta:check/build、两仓 git diff --check 均通过；后端 verify（3 个 Java 文件）、Maven compile、架构检查通过。真实 HTTP 核对 4 个样例接口的专家计数合计均等于接口汇总；专家、未归属、无数据、必填接口校验及主专家视图不受接口筛选影响均通过。1440/1920 均为 8 列、无横向滚动、表头完整；排序保持双行分组，错误率升降序缺失值置底、取消恢复默认顺序、长名称完整提示通过。截图 /tmp/expert-paired-1440.png、/tmp/expert-paired-1920.png。既有工作台资源 404 和构建分包告警保留。
