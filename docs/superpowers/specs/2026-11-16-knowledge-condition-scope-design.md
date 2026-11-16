# 知识条件适用范围

用户已在对话中确认本简短设计。

## 目标与边界

组件知识接口保留 conditions 名称，每项新增 id（适用关系主键）、targetType、targetId，供页面区分来源范围及模型识别适用依据。原有 unconditional、condition、exception、replacesKnowledgeId 保持不变。

知识正文按知识 ID 去重，不同适用关系即使条件相同也分别保留。conditions 只包含本次查询命中的关系，不能混入其他组件的反向引用。

保持现有召回范围，不执行自然语言条件或裁决冲突；不改数据库、relatedComponents 契约或前端页面。独立模型上下文接口不在本次改动范围内。

## 方案比较

讨论过按范围拆分知识列表、将 conditions 改为 applicabilities，以及沿用 conditions 增加范围字段。用户选择第三种，避免字段改名和正文重复，以最小变更保留关系身份。

## 实现与验证

修改 KnowledgeCondition 和 KnowledgeContextAssembler，将当前关系的身份和对象范围带入条件，并按关系 ID 去重。同步组件接口文档和说明性示例；历史真实响应保留原记录，不伪装为新接口验证结果。

执行修改 Java 文件的质量 verify、编译和架构检查。不新增或运行测试，不自动提交。工具不足或检查失败时如实报告。
