# Custom Java Rules for SonarQube

一个面向 SonarQube 的自定义 Java 代码质量规则插件，聚焦于 **数据库性能**、**事务可靠性** 以及 **MyBatis Mapper XML 中的 SQL 坏味道** 检测。规则在常规静态检查之外，补充了一批工程实践中高频出现、但默认规则集难以覆盖的隐患。

## 功能特性

- 基于 [SonarJava](https://github.com/SonarSource/sonar-java) 的语法树（AST）实现 Java 检查规则。
- 额外提供一个 Sensor，对 MyBatis Mapper XML 中的 SQL 进行规则扫描。
- 规则元数据通过 `sonar-analyzer-commons` 的 `RuleMetadataLoader` 加载，描述（HTML）与配置（JSON）与代码解耦。
- 所有规则默认严重级别为 `BLOCKER`。

## 规则清单

### Java 规则（仓库 key：`custom-java`）

| 规则 | 说明 |
| --- | --- |
| AvoidDatabaseQueryInLoop | 避免在循环中执行数据库查询（N+1 问题） |
| AvoidRemoteApiCallInLoop | 避免在循环中发起远程 API 调用 |
| LogExceptionWithStackTrace | 捕获 Exception 时应记录堆栈信息 |
| AvoidTransactionalSelfInvocation | 避免事务方法的自调用（导致事务失效） |
| AvoidInvalidTransactionalTarget | 避免在无效目标上使用 `@Transactional` |
| AvoidWriteInReadOnlyTransaction | 避免在只读事务中执行写操作 |
| AvoidCrossThreadWriteInTransactionalMethod | 避免在事务方法中跨线程写操作 |
| AvoidSwallowingExceptionInTransactionalMethod | 避免在事务方法中吞掉异常 |
| RequireRollbackForExceptionInTransactional | `@Transactional` 应显式声明 `rollbackFor` |
| AvoidClassLevelTransactional | 避免类级别的 `@Transactional` |
| AvoidTransactionalOnControllerMethod | 避免在 Controller 方法上使用 `@Transactional` |

### MyBatis Mapper XML 规则（仓库 key：`custom-mybatis-xml`）

| 规则 | 说明 |
| --- | --- |
| AvoidSelectStarInMybatisMapper | 避免在 SQL 中使用 `SELECT *` |
| AvoidInsertWithoutColumnsInMybatisMapper | 避免 `INSERT ... VALUES` 不显式指定列名 |
| AvoidPhysicalDeleteInMybatisMapper | 避免物理删除（建议逻辑删除） |
| AvoidUpdateOrDeleteWithoutWhereInMybatisMapper | 避免无 `WHERE` 条件的 `UPDATE`/`DELETE` |
| AvoidSqlBuiltinFunctionsInMybatisMapper | 避免使用非标准 SQL 函数 |
| AvoidExcessiveJoinsInMybatisMapper | 避免过多的 `JOIN` |
| AvoidExcessiveNestingInMybatisMapper | 避免过深的嵌套子查询 |
| AvoidOneEqualsOneConditionInMybatisMapper | 避免使用 `1=1` 作为查询条件 |
| AvoidSqlInjectionRiskInMybatisMapper | 避免使用 `${...}`，优先使用 `#{...}` 参数绑定 |

## 环境要求

- 构建 JDK：**JDK 8**
- 目标平台：SonarQube **8.9.x**（与服务端内置的 `sonar-java 6.15.1` 对齐）

## 构建

```bash
mvn clean package
```

构建产物为 `target/custom-java-rules-1.0.0-SNAPSHOT.jar`。

## 安装

1. 将构建出的 jar 拷贝到 SonarQube 的插件目录：

```text
<SONARQUBE_HOME>/extensions/plugins/
```

2. 重启 SonarQube 服务。
3. 在 **Quality Profiles** 中将 `Custom Java Rules` / `Custom MyBatis XML Rules` 仓库下的规则激活到对应的质量配置。

## 开发指南

- Java 检查类位于 `src/main/java/org/sonar/custom/java/checks/`，按 `performance` / `reliability` 分包。
- MyBatis XML 相关逻辑位于 `src/main/java/org/sonar/custom/java/xml/`。
- 新增规则时：
  1. 实现继承自 SonarJava `IssuableSubscriptionVisitor`（或等价基类）的检查类；
  2. 在 `CustomJavaRulesDefinition#getCheckClasses` 中注册（XML 规则在 `MybatisXmlRulesDefinition` 中注册）；
  3. 在 `src/main/resources/org/sonar/l10n/.../rules/custom/` 下添加同名的 `.json`（元数据）与 `.html`（描述）；
  4. 在 `src/test/` 下补充对应的单元测试与测试样例。
- 运行测试：

```bash
mvn test
```

## 许可证

本项目基于 [Apache License 2.0](./LICENSE) 开源。
