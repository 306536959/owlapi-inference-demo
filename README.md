# OWLAPI 推理物化测试（Protege 兼容思路）

这个工程用 **OWLAPI + HermiT 推理器** 来实现：
1) 你在代码/入参中提供 TBox/ABox（这里用 JSON）
2) 程序构建 `OWLOntology`
3) 调用推理器做一致性/分类/实例类型推断
4) 把“可枚举的推理结果”（实例类型、命名类的超类关系）物化为 OWL 公理
5) `save` 为 `output-inferred.owl`

> 说明：推理器无法把“所有蕴含”都穷举成有限 OWL，但对常见的可枚举结果（types / subclass hierarchy）能可靠落地。

## 运行

在项目根目录：

```powershell
mvn -q package
mvn -q exec:java -Dexec.args="--input src/main/resources/sample-input.json --output output-inferred.owl"
```

执行结束后会在项目根目录生成 `output-inferred.owl`。

## 输入 JSON 示例

`src/main/resources/sample-input.json` 已包含一个简单本体：
- `Man ⊑ Person`
- `Person ⊑ Mammal`
- `Man(a)` 推理出 `Person(a)`、`Mammal(a)`

你可以替换 JSON 中的 `axioms` 字段来表达你的领域公理。

## 无 CLI 的自动建模（Java 扫描数据库生成 OWL）

你可以直接通过 Java（JDBC + OWLAPI）扫描 MySQL 的表、字段、外键，并自动生成本体骨架。

执行示例：

```powershell
mvn -q exec:java "-Dexec.mainClass=com.example.owlapi.SchemaToOwlGenerator" "-Dexec.args=--jdbcUrl jdbc:mysql://127.0.0.1:3306/medical_demo --user root --password 123456 --baseIri http://example.com/medical# --output schema-auto.owl"
```

说明：
- 表 -> `owl:Class`
- 列 -> `owl:DatatypeProperty`（自动带 domain/range）
- 外键 -> `owl:ObjectProperty`（自动带 domain/range）

## Spring Boot API 服务（仅 SPARQL 查询接口）

启动：

```powershell
mvn spring-boot:run
```

健康检查：

`GET /api/health`

SPARQL 查询接口：

`POST /api/sparql/query`

请求体示例：

```json
{
  "query": "SELECT * WHERE { ?s ?p ?o } LIMIT 10"
}
```

## 内置配置（扫描表位置就在这里）

配置文件：`src/main/resources/application.yml`

关键字段：
- `ontology.bootstrap.jdbc-url`：扫描哪个数据库
- `ontology.bootstrap.schema`：扫描哪个 schema（可空）
- `ontology.bootstrap.table-pattern`：按表名模式扫描（默认 `%`）
- `ontology.bootstrap.include-tables`：白名单表名数组
- `ontology.bootstrap.exclude-tables`：黑名单表名数组
- `ontology.bootstrap.enabled`：`true` 时启动应用会自动扫描并生成 OWL

说明：
- 对外 API 只保留 SPARQL 查询接口。
- 映射配置和 OWL 可按系统内置方式管理；扫描行为由配置控制，不暴露为公开生成接口。
