# conditions 范围字段真实验证

日期：2026-11-16。

使用现有 request.json 调用本次代码启动的本地服务，不修改数据库数据。命令：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -B -Dmaven.repo.local="$PWD/.tool-cache/m2" spring-boot:run -Dspring-boot.run.arguments=--server.port=18082
curl --silent --show-error --max-time 15 -H 'Content-Type: application/json' --data-binary @docs/knowledge/validation/report-summary/request.json http://127.0.0.1:18082/api/v1/knowledge/component-context -o /private/tmp/zhituo-condition-scope-response.json
```

服务成功启动，curl 退出码 0。Python 对实际响应的断言退出码 0：

- code=200、success=true；业务知识13条、技术知识11条，知识ID无重复。
- conditions共45条：COMPONENT 19、PAGE 2、FLOW 2、INTERFACE 22。
- 每项id、targetType、targetId均为非空字符串，targetType属于四种已定义范围，每条知识内关系ID无重复。
- COMPONENT的targetId与当前组件数据库主键一致；PAGE与当前页面主键一致；INTERFACE在本次命中接口中。
- 21条知识存在多个关系，且保留条件内容相同但ID不同的关系，未因条件相同合并。
- relatedComponents为空；现有数据没有其他显式关联组件，未通过本次实际响应覆盖非空反向引用场景。
- issues中同一待确认知识产生两条KNOWLEDGE_PENDING提示，符合现有逐关系提示行为，未修改。

响应临时文件不属于持久验证产物。未新增测试、写入临时业务数据或改动历史响应文件；本记录不证明生产性能或所有边界场景。
