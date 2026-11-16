# 统一代码目录实施计划

**Goal:** 前后端目录统一存入knowledge_service并迁移现有数据库。
**Architecture:** 以service_type区分FRONTEND/BACKEND，保留后端ID和接口关联。
**Tech Stack:** Java 8、MyBatis XML、MySQL 8。
**Spec:** docs/superpowers/specs/2026-11-16-code-locations-design.md（已获用户确认）

## 约束
保留用户现有改动；不新增或运行测试，不自动提交。仅检查本次代码；编译和架构检查覆盖项目。由当前代理连续实施。

## 步骤
- [x] 修改knowledge-schema.sql、component-knowledge.sql和种子SQL；增加分阶段迁移SQL。核对同项目同服务标识可按类型并存。
- [x] 修改KnowledgeMapper.xml、KnowledgePo、KnowledgeItem及转换器，目录查询携带类型和系统项目，接口限定BACKEND。
- [x] 修改组件上下文、模型上下文查询及目录DTO，统一读取服务表；同步两份接口文档。
- [x] 对本次Java文件执行java-quality verify、check和format-check，使用项目Java 8编译，运行architecture-check。
- [x] 备份受影响表数据，执行迁移第一阶段；逐项核对目录、后端记录及接口关联，再删除旧列并核对最终结构。

## 重点核对
空目录保留缺口；前后端同项目号和同服务标识不混淆；未请求接口时不枚举后端；项目不存在提示保留；数据库DDL隐式提交，分阶段核对。

## 验证记录

- 8个修改Java文件：java-quality.sh verify、format-check通过；已有@author/@date提示为info，0 Checkstyle违规。
- 使用README指定Java 8执行mvn -B -Dmaven.repo.local=.tool-cache/m2 -DskipTests compile，通过（158个源文件）。
- python3 scripts/architecture-check.py .，通过（脚本内部执行架构规则检查）；未新增或运行单元/e2e测试。
- Mapper XML解析、两份DDL字段及唯一键核对通过。
- 数据库已备份并实际迁移：项目1、前端1、后端1、接口1；目录逐项一致，后端记录与接口关联未变，旧列删除。报告见docs/knowledge/validation/code-location-migration.json。
- 本机8080无监听服务，无需重启已有进程；未执行HTTP接口测试。
- 独立只读代码审查完成，未发现本次改动引入的实质问题或阻塞项。
