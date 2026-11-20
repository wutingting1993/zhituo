# Codex 自动检查 Hook

## 引入与启用

1. 将模板的 `.codex/` 与 scripts/、config/、tools/ 复制到目标 Git 仓库根目录。已有 hook 必须合并，避免重复执行；不要覆盖已有配置。
2. 按项目修改 `.codex/quality.json`：

```json
{
  "project_dir": "backend",
  "source_roots": ["config-service/src/main/java", "engine-service/src/main/java"]
}
```

project_dir 相对 Git 根目录，source_roots 相对 project_dir。单模块项目使用 `.` 和 `src/main/java`。当前 Stop 构建入口适用于 Maven 项目；Gradle 或特殊 profile 项目须先适配 backend-gate.sh。

3. 在项目 `.gitignore` 添加 `.tool-cache/`，配置项目 JDK、Maven、Python 3，先手动运行完整 gate，确保依赖可下载和规则适用。
4. 重新加载项目后审查并信任 hook。Codex 桌面端使用 Settings > Hooks 或 Review hooks；CLI 使用 `/hooks`。项目必须受信任，修改 hook 后需要重新审查。

## 执行行为

- PostToolUse 不限定工具名，按 Git 状态检测新增、暂存、未暂存的 Java 文件，不依赖 apply_patch 参数；内容相同且已通过时复用结果。
- Stop 检测 Java、构建及质量配置变化，运行只读完整 gate：Checkstyle → 格式一致性 → `mvn clean compile` → 架构检查。
- 检查记录关联当前 HEAD、改动路径及内容、JAVA_HOME 和 PATH；代码变化、删除或环境路径变化使记录失效。缓存是本地优化，不作为防篡改证明。
- 无相关改动时跳过；纯文档不触发构建。检查覆盖当前工作区改动，可能包含用户已有未提交内容，不限于本轮模型修改。
- 工具、配置缺失或执行超时视为未通过。日志位于 `.tool-cache/hooks/post.log`、`stop.log`。
- Stop 首次失败要求模型继续修复，再次失败会停止并明确标记任务未完成，防止无限续跑；不是检查放行。用户明确中断始终优先。
- 超过 540 秒的完整构建需要项目调整脚本及 hook 超时；依赖下载建议提前完成。

## 手动验证

```bash
# 在目标项目根目录执行，按真实项目修改 quality.json
printf '{"hook_event_name":"PostToolUse"}' | python3 .codex/hooks/quality_hook.py
printf '{"hook_event_name":"Stop","stop_hook_active":false}' | python3 .codex/hooks/quality_hook.py
```

Stop 失败使用 JSON `decision: block` 反馈；JSON 正确输出时进程可返回 0，不能只凭退出码判断放行。Hook 仅在启用并受信任的 Codex 会话中自动运行，其他编辑器和手动修改仍由 CI 兜底；合并必需检查需在仓库侧配置。

官方说明：[Codex Hooks](https://learn.chatgpt.com/docs/hooks)。

初始化验收：后端须先实际运行完整 gate，再验证 PostToolUse、Stop 和 Codex 中的真实触发状态；手动验证时确认没有复用旧凭证。完整门禁不负责启动服务，后端与前端须另外按 INIT.md 完成启动及接口/页面验证。
