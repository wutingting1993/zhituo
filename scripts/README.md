# Java 格式化与检查

无需安装 IDEA 或 Eclipse。需要 Python 3、Maven 3.6.3+ 及与项目、质量工具兼容的 JDK；其他机器不强制 JDK 版本，沿用项目环境。

```bash
bash /absolute/path/规范/scripts/java-quality.sh format src/Example.java
bash /absolute/path/规范/scripts/java-quality.sh check src/Example.java
bash /absolute/path/规范/scripts/java-quality.sh verify src/Example.java src/Other.java
```

- format：Spotless 2.30.0 + Eclipse JDT 4.16.0，应用 config/eclipse-formatter.xml。
- check：Maven Checkstyle 插件 3.3.1 + Checkstyle 9.3，使用 config/checkstyle.xml。
- verify：先格式化再检查，全部通过后写回。
- 首次执行需联网下载固定版本依赖，缓存位于 .tool-cache/m2。
- 仅处理显式传入文件的临时副本；失败不写回，成功时仅更新发生变化的文件。项目 POM 不受影响。
- 配置转换覆盖行宽、缩进、括号、注解及空行，不保证与 IDEA 完全一致；codestyle.xml 保留供 IDEA 使用。
- 格式化不会自动补齐所有语义规则，例如缺失的大括号、命名及复杂度问题，须根据检查结果修正。
- 复制模板时保留 AGENTS.md、docs/、config/、scripts/、tools/，缓存无需复制。

JDK 8 版本组合：Spotless 2.30.0、Eclipse JDT 4.16.0、Checkstyle 9.3。本机已找到并验证 Zulu JDK 1.8.0_512，java、javac 和 Maven 均确认使用 JDK 8；工具组合已在本机 JDK 8 实际运行通过。

批量执行（在目标项目根目录）：

```bash
export JAVA_HOME="/path/to/project-jdk"
export PATH="$JAVA_HOME/bin:$PATH"
bash scripts/java-quality.sh check backend/
bash scripts/java-quality.sh verify module-a/src/ module-b/src/
```

目录参数递归扫描 Java 文件，跳过 target、build、node_modules、dist、.git、.tool-cache 及符号链接；支持目录与文件混合传入。脚本不强制 JDK 版本；工具不兼容时说明原因，不擅自修改项目 JDK。


## 自动选取与合并检查

```bash
# 暂存、未暂存和未跟踪的 Java 改动
bash scripts/java-quality.sh verify --changed
# 加上相对指定分支的变更（CI 需先拉取该 ref）
bash scripts/java-quality.sh check --base origin/main
# 只读格式一致性检查
bash scripts/java-quality.sh format-check backend/
# 静态检查 + 格式检查 + 项目编译 + 架构检查
bash scripts/backend-gate.sh backend/
```

--changed 与 --base 不能与文件参数混用；无 Java 变更时明确跳过，不等于项目全量检查通过。CI 接入见 [CI.md](CI.md)。当前配置面向 Java 8 语法，新项目采用更高 Java 语法时，必须升级格式化、检查和架构工具并复验，不只更换运行 JDK。

验证记录：JDK 8 下工具代码自身通过 verify；临时 Maven 项目通过完整 gate；架构合规样例通过，领域直连 Mapper 和服务缺少接口样例被阻断。工具自检命令：`python3 -m unittest discover -s tests`。

自动触发接入：[Codex Hook 配置](HOOKS.md)。模板不自动安装到其他项目，也不自动授予 hook 信任。

新项目初始化：[命令及生成结构](INIT.md)。通用返回、错误码和业务异常直接维护 scripts/templates/ 下的 Java 文件，初始化复制时替换包名。
