# 项目初始化指南（供模型执行）

用户要求“按本文初始化项目”时，按以下流程完成信息收集、初始化与验证。本文是执行指南，不是仅供展示的命令说明。

**目标目录必须由用户指定。未提供时先询问，禁止默认使用当前工作目录；相对路径先说明解析后的绝对路径并请用户确认，已明确的绝对路径无需重复确认。目标目录确定前不执行写入。**

## 1. 检查现状

读取用户已提供的信息，检查目标目录、已有规范、构建文件、包名、模块、JDK 及 Git 状态。仅检查初始化相关文件，保留现有代码和配置。

- 新项目：确定是否从空目录创建可运行骨架，还是仅引入规范。
- 已有项目：优先采用现有技术栈、版本、包名与模块结构。
- 已知信息直接复用，缺失或冲突的信息集中向用户询问。

## 2. 补齐初始化信息

只询问无法从用户指令、项目文件或环境中确认的内容。一次集中询问，提供简短选项；用户不熟悉时给出建议并说明影响。必需信息未明确前先完成只读检查。

| 信息 | 需要确定的内容 |
|---|---|
| 目标与范围 | 项目名称、绝对路径；新建骨架或接入规范；后端、前端或前后端 |
| 后端技术栈 | Spring Boot/Spring 版本、JDK、Maven/Gradle；已有项目沿用配置 |
| 包与模块 | Java 基础包名、单/多模块、后端构建目录、各模块源码目录 |
| 通用类归属 | 多模块中由哪个模块承载通用返回、错误码、异常及 ID 工具 |
| 项目依赖 | 是否涉及数据库写入；数据库及驱动、Web/Validation、Lombok 等按实际需要配置 |
| 前端（如涉及） | 框架、版本、包管理器、前端目录；仅后端任务跳过 |
| 工具与接入 | 实际 JDK 路径、Git 是否初始化、是否接入已有 CI；hook 需用户在 Codex 中审查信任 |

询问示例（按已知信息删减，不原样重复询问）：

> 还需补充：
> 1. 项目保存路径和 Java 基础包名是什么？
> 2. 是仅接入规范，还是创建后端/前后端可运行骨架？
> 3. 使用哪个 JDK、Spring Boot 版本和构建工具？
> 4. 单模块还是多模块？是否需要数据库？

收齐信息后简要说明实际参数与改动范围，直接执行已授权的初始化；涉及覆盖、删除或尚未授权的扩展时才单独确认。

## 3. 执行初始化

### 3.1 运行规范初始化脚本

在本文所在目录定位 [init-project.sh](init-project.sh)，使用其绝对路径执行；路径含空格时加引号。

```bash
# 单模块：project-dir 默认为 .，source-root 默认为 src/main/java
bash /path/to/规范/scripts/init-project.sh /path/to/project \
  --base-package com.example.orders

# 后端位于 backend，源码路径相对 backend
bash /path/to/规范/scripts/init-project.sh /path/to/project \
  --base-package com.example.orders \
  --project-dir backend --source-root src/main/java
```

将示例替换为已确定的实际参数。多模块可重复传入 `--source-root`；通用类只生成在第一个源码目录，其他模块通过项目依赖复用。首个源码目录必须属于已确定的公共模块或承载模块，不能任意选择。

脚本会：

- 复制 AGENTS.md、前后端规范、config/、scripts/、tools/、tests/ 和 .codex/；更新 .gitignore，缓存不复制。
- 在指定基础包下创建 adapter、application、domain、infrastructure 四层的 package-info.java。
- 复制以下真实 Java 文件，并将 `template.base` 替换为实际基础包，包含包声明和 import。

| 模板 | 目标包 |
|---|---|
| [GlobalExceptionHandler.java](templates/GlobalExceptionHandler.java) | 基础包.adapter.web.advice |
| [BusinessException.java](templates/BusinessException.java) | 基础包.infrastructure.common.exception |
| [ResponseEnum.java](templates/ResponseEnum.java) | 基础包.infrastructure.common |
| [Result.java](templates/Result.java) | 基础包.application.api.dto |
| [CUuid.java](templates/CUuid.java)、[HexEncoding.java](templates/HexEncoding.java) | 基础包.infrastructure.common.util |

已有目标文件时脚本整批停止。检查冲突后保留已有有效内容，手工合并本任务需要的规范或工具配置；合并后直接执行后续验证，禁止删除已有文件以强行重跑。

### 3.2 完成项目实际配置

脚本仅安装规范与 Java 骨架，**不创建业务 POM、前端应用、数据库连接、Git 仓库或 CI 配置，也不安装 JDK**。模型须根据第 2 步确定的范围补齐：

- 仅接入规范：沿用现有构建、依赖和模块，按需适配模板。
- 空项目可运行骨架：创建用户选定的构建文件、模块依赖和启动入口；前端及数据库仅在已确定需要时初始化。
- 配置 Lombok 注解处理及 Web、Validation、Jackson、SLF4J 等必要依赖；版本服从项目依赖管理。
- 默认异常模板适用于 Spring MVC 5.3、javax.validation。Spring Boot 3+ 切换 jakarta.validation 并适配异常 API；WebFlux 项目需改用对应异常及处理机制，不能直接套用 MVC 模板。
- JDK 按目标项目要求选择，本机路径只作环境记录，其他机器不强制版本。当前质量工具已验证 Java 8；新 Java 语法需选用兼容的工具版本并重新验证。
- 确认启动包扫描覆盖生成代码；调整 .codex/quality.json 的 project_dir、source_roots，并按 [HOOKS.md](HOOKS.md) 配置 hook。
- Maven 项目使用现有 gate；Gradle 项目适配编译和 gate 命令。Git/CI 仅在用户授权范围内配置。

通用约定：HTTP 200；code 为 int，成功 200，异常从 10000 开始，未知异常 99999；包装业务异常保留原 cause；写入需要生成 ID 时使用 CUuid.nextId()。MVC 路由错误及过滤器错误须按项目配置接入，不能因存在全局处理器就宣称全部覆盖。

## 4. 运行验证（完成条件）

初始化涉及哪一端，就必须启动并验证哪一端；前后端项目两端都要验证。已有项目仅接入规范时也执行适用的启动与检查。工具、依赖或环境缺失时报告阻塞，禁止宣称全部完成。

### 后端

1. 核对实际 JDK、Maven/Gradle、依赖解析及必要数据库等环境，使用项目要求的版本。
2. 对新增、修改的源码执行格式化与静态检查，再执行完整构建门禁：

```bash
bash scripts/java-quality.sh verify path/to/source/
bash scripts/backend-gate.sh path/to/source/
```

脚本路径相对于规范安装目录，源码参数相对于实际执行目录；Maven gate 在业务 POM 所在目录运行。多模块包含实际源码和全部关联模块编译产物。Gradle 项目使用已适配的构建入口。

3. 按 [HOOKS.md](HOOKS.md) 验证 PostToolUse 和 Stop，检查日志、退出结果及 JSON 决策，确认实际执行了质量检查、格式检查、编译和架构检查。首次接入应清除目标项目的旧 hook 凭证后验证；模板不携带缓存。
4. 在 Codex 中审查信任 hook 后，确认真实事件能够触发。手动运行只证明脚本可执行，不代表 Codex 自动触发已生效；尚未信任时标记为待用户操作，禁止自动授信。
5. 使用项目实际命令和环境配置启动服务，等待启动完成，确认进程、监听端口及启动日志；请求已有的健康接口或可验证接口，检查预期响应。没有健康接口时使用实际可用接口，不因验证擅自增加接口。

### 前端

1. 使用项目包管理器安装依赖，执行已有 lint、类型检查及构建脚本。
2. 使用实际开发命令启动服务，确认端口和访问地址；打开页面，检查加载结果、浏览器控制台及资源请求。
3. 前后端项目还需验证代理或 API 地址，完成一次实际接口调用；禁止仅凭首页返回 HTTP 200 判断联调成功。

### 进程与交付

- 启动端口和环境优先读取项目配置；存在冲突时说明占用情况，禁止终止无关进程或静默换端口。
- 记录本次创建服务的命令、PID/会话、日志及停止方式；遵从用户要求保留或停止，禁止将临时启动验证表述为部署完成。
- 交付说明实际检查命令与结果、前后端访问地址、运行状态、hook 自动触发状态及未完成事项。已有服务无法重启或缺少外部环境时明确报告，禁止使用模拟结果替代。

## 禁止项

- ❌ 禁止猜测目标路径、基础包名、模块归属或替用户默认选择技术栈。
- ❌ 禁止重复询问已有明确答案的信息。
- ❌ 禁止覆盖已有代码、构建文件、规范或 hook 配置。
- ❌ 禁止因初始化自动增加未要求的业务功能、数据库操作、权限或远程仓库操作。
- ❌ 禁止仅复制模板就宣称可运行骨架已完成。
- ❌ 禁止未验证、验证失败或仍有阻塞时宣称初始化全部完成。
- ❌ 禁止未实际启动适用的前后端服务就宣称初始化全部完成。
- ❌ 禁止未执行后端质量门禁及 hook 验证就宣称后端环境正确。
