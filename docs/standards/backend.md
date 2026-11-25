

# 后端与数据库规范

适用于采用 Java / Spring Boot 的项目 后端及 SQL、DDL、数据库变更；与根目录 [AGENTS.md](../../AGENTS.md) 一并遵守，全局约束和优先级以入口文件为准。

## 后端代码规范：Java / Spring Boot

字段注入、Lombok、Guava 和 MyBatis XML 为本团队 Java/Spring Boot 项目约定；其他技术栈仅适用对应的通用原则。

### 后端代码结构

- ❌ 禁止创建空目录凑齐结构。
- ❌ 禁止因目录示例引入依赖或替换现有客户端。
- ❌ 禁止附带重构无关代码。

以下目录位于项目 Java 基础包下。`module1`、`module2` 为业务模块示例名，实际使用业务名称；方括号表示可选分包，不是实际目录名。

```text
基础包/
├── adapter/                         # 适配器层：请求入口
│   ├── [module1]/                   # 按业务模块分包，可选
│   └── [module2]/
│       └── web/                     # Web 请求入口
├── application/                     # 应用层
│   ├── [module1]/                   # 按业务模块分包，可选
│   └── [module2]/
│       ├── api/
│       │   └── dto/
│       ├── service/
│       └── support/
├── domain/                          # 领域层
│   ├── module1/
│   └── module2/
│       ├── model/
│       │   ├── [aggregate1]/        # 聚合含多个实体时，可按聚合分包
│       │   ├── [aggregate2]/
│       │   └── User.java            # 未使用聚合或聚合仅含一个实体时直接放 model 下
│       ├── service/
│       └── support/
└── infrastructure/                  # 基础设施层
    ├── common/                      # 所有层都可以依赖的公共类
    │   ├── config/
    │   ├── constant/
    │   └── util/
    ├── client/                      # 云服务、远程调用客户端
    │   ├── client/                   # 远程调用，按需
    │   │   ├── converter/
    │   │   └── dto/
    │   └── redis/                   # Redis 云服务，按需
    │       ├── config/
    │       ├── converter/
    │       └── dto/
    ├── [module1]/                   # 模块分包，与领域层模块对应
    └── [module2]/
        ├── dao/
        │   ├── converter/           # 持久化模型 PO 与实体转换
        │   ├── mapper/              # Mapper
        │   └── po/                  # 持久化模型 PO
        └── supportimpl/
```

- 新增后端代码按上述职责归属放置；可选分包及聚合分包按实际需要建立；client、Redis 目录仅在项目实际使用对应客户端时建立；已有项目采用不同结构时，先遵循现有模块边界；目录迁移须有明确任务要求。

### 分层调用备注

- 典型调用方向：adapter → application → domain；应用层负责用例编排，领域层负责业务规则，简单业务不必机械经过所有类型的类。
- 领域需要持久化或外部能力时，通过 domain.support 接口调用，由 infrastructure.supportimpl 实现；应用 DTO、领域模型、持久化 PO、外部服务 DTO 按各自职责使用，在对应边界转换。
- application.support 仅放应用编排所需的辅助能力；领域业务依赖的能力接口归 domain.support，避免职责重叠；infrastructure.common 中供各层使用的内容应保持无业务含义。

- 服务边界接口属于必要契约，不属于单次使用的辅助抽象；应用层和领域层面向接口编程：应用服务接口放在 application.api，实现放在 application.service；领域服务在 domain.service 中定义接口与实现，外部能力接口放在 domain.support。
- 调用方通过接口声明依赖，具体实现由 Spring 注入；领域模型、DTO、PO 等数据对象按职责使用具体类型。

- ❌ 禁止 domain 直接依赖 Mapper、PO、client 或 Redis 等技术实现。
- ❌ 禁止 adapter 绕过应用层直接调用 Mapper 或客户端。
- ❌ 禁止 application 依赖 adapter 或 infrastructure 的具体技术实现（公共类除外）。
- ❌ 禁止 domain 依赖 application 或 adapter。
- ❌ 禁止混用应用 DTO、领域模型、持久化 PO 和外部服务 DTO。
- ❌ 禁止领域层引用 infrastructure.common 中的数据库、Redis 等技术配置。
- ❌ 禁止 Controller 或其他调用方直接依赖应用服务实现类。
- ❌ 禁止应用层、领域层直接依赖协作服务或支持能力的实现类。

### 分层与接口契约

- REST 接口风格，统一分页、统一返回体，使用 @Valid 做入参校验；接口前缀沿用项目约定，新项目可采用 `/api/v1/`；Controller 仅接收请求、声明参数校验并委派应用层；请求与响应 DTO 独立保存，导出等协议处理放入单独组件。
- HTTP 异常处理、导出协议处理及入口 DTO 转换归属 adapter。

- ❌ 禁止 Controller 处理业务逻辑。
- ❌ 禁止 Controller 使用 try-catch、@ExceptionHandler 或其他方式捕获、转换异常。
- ❌ 禁止应用层反向引用 adapter 的 DTO。

### 统一返回结构

- 应用可控制的 JSON 接口（含过滤器、安全处理器的错误出口）正常及异常响应的 HTTP Status 始终为 200，通过 code 区分结果：成功为 200（整数），失败使用业务错误码，success 与 code 保持一致；错误码及默认文案由 ResponseEnum 集中管理。
- 异常错误码按目标项目约定使用；使用 Result.success(data) 或 Result.failure(code, message, data)；无数据时 data 为 null。

```java
@Getter
@Builder
public final class Result<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ResponseEnum.SUCCESS.getCode())
                .message(ResponseEnum.SUCCESS.getMessage())
                .data(data)
                .success(true)
                .build();
    }

    public static <T> Result<T> failure(int code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .success(false)
                .build();
    }
}
```

文件下载、流式响应按独立协议处理；网关、容器等应用外部错误不由 Result 契约保证。

### 异常处理与日志

- 异常向上抛出；转换为 BusinessException 时，必须将捕获的原异常传入 cause，由 super(message, cause) 保存。主动业务校验失败且没有原异常时才使用无 cause 构造。
- 业务异常在 application、domain、infrastructure 等层按职责处理：application 处理用例及流程约束，domain 处理业务规则，infrastructure 将持久化、外部调用等技术异常转换为上层可理解的异常。
- 无法在职责层内恢复的异常向上抛出，由独立的 @RestControllerAdvice 全局异常处理器统一记录日志并转换响应；全局异常处理器统一记录一次：业务异常、参数异常用 warn，非预期异常用 error；恢复后不再抛出时在恢复边界记录。
- 日志包含业务上下文、原始错误信息及完整堆栈，异常对象放最后；敏感信息先脱敏；参数异常提取实际原因：对象校验提取字段及校验消息，约束校验提取属性路径及消息，类型转换或请求体解析失败提取参数名、字段路径及类型要求。

- ❌ 禁止吞异常、空 catch、printStackTrace() 和捕获 Throwable。
- ❌ 禁止包装已有异常时只复制消息而丢弃原异常 cause。
- ❌ 禁止无恢复、转换或补充上下文需要的逐层重复捕获。
- ❌ 禁止直接返回笼统的“参数错误”。
- ❌ 禁止暴露原始请求值或内部异常。

全局处理器代码见 [GlobalExceptionHandler.java](../../scripts/templates/GlobalExceptionHandler.java)，初始化时复制到 adapter.web.advice；Controller 无需捕获异常。

异常错误码及取值由目标项目的 ResponseEnum 统一维护，本规范不枚举。业务消息及校验消息须可安全展示，不包含敏感参数值。

模板基于 Spring MVC 5.3 / javax.validation；其他 Spring 版本按实际 API 调整。路由不存在须由 MVC 抛出 NoHandlerFoundException 才进入处理器，过滤器、安全组件及容器错误须按项目入口单独适配。

### Spring 组件与依赖注入

- 应用服务使用 @Service，基础组件使用 @Component，依赖由 Spring 注入；@Bean 仅用于第三方对象或配置装配；Spring 依赖在成员字段上使用 @Autowired；配置值在字段上使用 @Value。
- 需要依赖就绪后执行的资源装配放在 @PostConstruct 方法中。

- ❌ 禁止通过 Environment.getProperty 或 Binder 动态读取配置值。

### 数据模型与对象构建

- 字段结构已知的请求、响应和业务数据必须使用明确的 Java 类型。Web 接口、应用服务、领域数据访问接口和外部服务适配器按各自边界声明类型，集合和响应信封须声明具体的泛型参数。
- 在序列化与反序列化边界处理 JSON 字段名、数值类型和可空性。业务逻辑通过对象访问器读取数据。Mock 实现使用与真实数据源相同的类型契约。
- 禁止用 `JsonNode`、`ObjectNode`、`ArrayNode`、`Map<String, Object>` 或 `Object` 代替结构已知的业务对象。结构由外部动态决定或需要保留原始 JSON 时，可以在对应边界使用 JSON 树，并明确它所承载的数据范围。

- 数据对象使用 @Builder 和 builder() 按字段名初始化。

- 数据模型采用 Lombok @Getter/@Setter，字段私有化，通过访问器读写；需要生成构造方法时，使用 Lombok @NoArgsConstructor、@AllArgsConstructor；后续业务状态变更仍可使用 setter。
- 集合字段通过 @Builder.Default 保留 Guava 创建的独立默认集合，集合创建方式遵守下节规范；非空数据优先使用基本类型，可空数据使用包装类型；Lombok、Jackson 等注解按实际需要使用。

- ❌ 禁止依赖字段声明顺序调用 @AllArgsConstructor、@RequiredArgsConstructor 生成的构造器，避免字段调整后同类型参数错位。
- ❌ 禁止 new 后连续 setter 补齐初始值。
- ❌ 禁止添加无实际用途的 Lombok、Jackson 注解、构造器或序列化配置。

### 集合、类型安全与可变性

- Guava 仅用于集合工具；空集合通过 Lists.newArrayList()、Maps.newHashMap()/newLinkedHashMap()/newConcurrentMap()、Sets.newHashSet() 等创建；集合必须声明泛型。
- 强制类型转换前必须确认类型安全；@SuppressWarnings 限定在最小范围，并注释说明必要性；返回集合的方法无结果时返回空集合。

- ❌ 禁止使用 Guava Cache。
- ❌ 禁止使用 new ArrayList<>()、new HashMap<>()、new HashSet<>() 等空集合构造。
- ❌ 禁止使用 List、Map 等原始类型。
- ❌ 禁止返回集合的方法在无结果时返回 null。
- ❌ 禁止直接暴露内部可变集合。
- ❌ 禁止修改调用方传入的集合或对象，除非方法命名或中文 Javadoc 已明确声明该行为。

### 序列化与 HTTP 调用

- 使用 Jackson；共用 Spring Boot 托管的 ObjectMapper，各组件直接使用 @Autowired 注入；HTTP 调用沿用项目现有客户端。
- 使用 WebClient 时，复用超时配置和上述 ObjectMapper，保留现有并发、容量限制与取消行为。

- ❌ 禁止 Fastjson。
- ❌ 禁止自行 new 或 copy ObjectMapper。

### 持久化与配置

- 持久化使用 MyBatis Mapper 接口和 XML 手写 SQL；Mapper 及实现属于 infrastructure 层；运行配置使用 YAML，按项目约定区分不同环境。
- 多表查询根据业务语义、数据规模及执行计划选择 JOIN、子查询或拆分查询，具体约束见下文数据库规范；数据库命名使用下划线，实体属性使用小驼峰。

- ❌ 禁止使用 MyBatis-Plus 单表 API、注解 SQL 或 JdbcTemplate。

### 代码质量与静态检查

- Java 代码任务交付前必须对本次新增、修改的 Java 文件执行 verify；修正后重跑，退出码 0 方可宣称完成，并报告实际命令与结果。

- 格式化使用 [Eclipse 配置](../../config/eclipse-formatter.xml)，检查使用 [Checkstyle 配置](../../config/checkstyle.xml)。
- 环境：Python 3、Maven 3.6.3+；JDK 按项目和工具兼容性选择，其他机器不强制版本。首次执行需联网。
- 人工核对：减少嵌套、中文 Javadoc、按需导入与成员声明。

在项目根目录执行；文件或目录均可传入，目录递归扫描 Java 文件：

```bash
# 本机环境示例；其他机器按项目要求设置 JDK
export JAVA_HOME="/path/to/project-jdk"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version

# 批量检查后端全部 Java 文件，不修改文件
bash scripts/java-quality.sh check backend/

# 批量格式化并检查，全部通过后写回
bash scripts/java-quality.sh verify backend/

# 指定多个目录或文件
bash scripts/java-quality.sh check module-a/src/ module-b/src/ path/to/Changed.java

# 未复制模板时，使用通用规范中的脚本
bash ./scripts/java-quality.sh check backend/
```

verify 可替换为 format（仅格式化）或 check（仅检查）。路径相对于当前目录，含空格时加双引号；目录扫描跳过 target、build、node_modules、dist、.git、.tool-cache 及符号链接。退出码 0 表示通过，失败不写回；只处理指定范围，全量检查仅在明确要求时执行。

- ❌ 禁止跳过检查、降低检查级别或滥用抑制注解掩盖违规。
- ❌ 禁止将仅格式化通过视为检查通过。
- ❌ 禁止 lombok.val、单行方法体或条件块、类体首行空行。
- ❌ 禁止重复计算循环不变量或重复解析相同 JSON、表达式。
- ❌ 禁止引入未使用的成员、变量或注释掉的代码。
- ❌ 禁止清理无关历史代码。
- ❌ 禁止模板注释或逐句翻译代码。

hook 自动触发与启用见 [HOOKS.md](../../scripts/HOOKS.md)；未启用时手动执行以下命令。

#### 交付与构建检查

```bash
# 自动检查本次 Git 改动
bash scripts/java-quality.sh verify --changed
# CI：只读静态检查、格式检查、业务编译及架构检查
bash scripts/backend-gate.sh backend/
```

架构规则基于编译后类，按规范包结构执行；接入项目构建和 CI 见 [说明](../../scripts/CI.md)。当前工具组合已用 JDK 8 验证；新 Java 语法需升级工具并复验，其他机器不强制 JDK 版本。

### 构建运行


- JDK 版本按项目要求选择；执行前核对 JAVA_HOME、java -version 及构建工具实际版本，缺少指定版本时说明阻塞。
- 构建和启动命令以项目配置为准；已配置检查钩子时遵守其要求。

- ❌ 禁止假定项目已安装特定工具或钩子。
- ❌ 禁止擅自升级或降级项目指定的 JDK 版本。

## 数据库规范（强制）
- ✅ 每张数据表必须定义主键，新增数据写入需要生成主键或业务 ID 时，统一使用 CUuid.nextId()；✅ 所有数据表、所有字段必须添加COMMENT注释，说明业务含义；字符串统一用 varchar，长文本用 text。
- 索引只加在业务查询、过滤字段上，按需建立。

### 查询实现与风险评估

- 按业务语义、数据规模和执行计划选择 JOIN、子查询或拆分查询；允许数据库支持的跨表子查询、EXISTS、派生表和 CTE，不以表数单独判断合理性。
- 统计、去重、排序及分页优先在数据库完成；展示数据可分页后批量补充关联信息，避免逐条查询。关联统计须明确粒度，防止重复计数或累加。
- 超过 3 张表须说明关联关系、过滤范围和索引情况，并用 EXPLAIN 检查，无须仅因表数审批。
- 大范围扫描、中间结果显著膨胀或超出项目性能预算时，先优化；仍需采用时，说明原因、影响和替代方案，取得用户确认。无性能预算时按具体风险确认，不统一限定扫描行数。
- 缺少执行计划或代表性数据验证时，说明未验证范围，不宣称性能达标。

- ❌ 禁止为规避表数限制，将无明确规模上限的明细加载到内存进行统计、排序或分页。
- ❌ 禁止无关联条件的笛卡尔积，或通过嵌套、拆分、包装规避查询评估。

### 数据库其他约束

- 建表 SQL 保存到项目约定的 SQL 目录，表和字段带中文 COMMENT；临时内存数据库仅用于测试。

- ID 工具模板：[CUuid.java](../../scripts/templates/CUuid.java)，位于 infrastructure.common.util；依赖的 HexEncoding 随初始化一并生成。新增数据写入需要生成 ID 时，在写入前调用 CUuid.nextId()；更新或已有 ID 的写入沿用原 ID。

- ❌ 禁止需要生成数据 ID 时改用 UUID.randomUUID()、随机数或自行拼接 ID 替代 CUuid。
- ❌ 禁止字段类型：ENUM、SET、JSON、LOB、BLOB、CLOB。
- ❌ 禁止定义外键（FOREIGN KEY）、级联删除或级联更新。
- ❌ 禁止存储过程、自定义函数、触发器、定时作业、视图。
- ❌ 禁止使用 `select *`。
- ❌ 禁止盲目加索引。
- ❌ 禁止对外部数据库自动执行建表。
- ❌ 禁止用测试身份或模拟结果回退处理生产请求。
