# 统一前后端代码目录

用户已确认：扩展knowledge_service统一保存代码目录，增加service_type（FRONTEND/BACKEND），删除knowledge_project.local_path，并执行数据库迁移。前后端项目号可以相同，唯一键包含类型；service_code也允许跨类型相同。复用原表可保留后端ID及接口关联；新建仓库表会扩大迁移范围，不采用。

前端记录沿用项目ID和项目号作为服务标识，不生成新ID。组件和模型上下文目录返回serviceType；接口只匹配BACKEND。不新增接口、UI、依赖、测试或权限。仅执行必要静态检查、编译、架构检查及数据库迁移数据核对。迁移先复制核对，再删旧列；先备份受影响数据。
