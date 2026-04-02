# OWLAPI GraphDB 管理平台

基于 Spring Boot 的知识图谱管理平台，支持从关系数据库自动生成 OWL 本体和 OBDA 映射文件，并与 GraphDB 无缝集成。

## 核心功能

- **Schema 扫描**: 连接关系数据库，自动扫描表结构和外键关系
- **本体生成**: 根据数据库 Schema 自动生成 OWL 本体
- **映射生成**: 生成 OBDA 映射文件，实现关系数据到 RDF 的虚拟映射
- **GraphDB 管理**: 提供完整的 GraphDB 仓库管理功能
- **SPARQL 查询**: 支持执行各种 SPARQL 查询
- **Web UI**: 现代化的管理界面，无需操作 GraphDB 后台

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.x
- GraphDB (可选)

### 启动应用

```powershell
cd c:\Users\tzp\owlapi-inference-demo
mvn spring-boot:run
```

访问: http://localhost:8083

### 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
ontology:
  bootstrap:
    enabled: true
    jdbc-url: jdbc:mysql://127.0.0.1:3306/your_database
    user: root
    password: your_password
    base-iri: http://example.com/ontology#
  
  graph-db:
    url: http://localhost:7200
    repository-id: your_repo
```

## 项目架构

```
src/main/java/com/example/owlapi/
├── OwlApiSpringApplication.java      # Spring Boot 启动类
├── SchemaToOwlGenerator.java         # OWL 本体生成器
├── SchemaToObdaGenerator.java        # OBDA 映射生成器
├── api/                              # REST API
│   ├── OntologyController.java       # 本体操作 API
│   ├── GraphDbController.java       # GraphDB 管理 API
│   └── SparqlGatewayService.java    # SPARQL 网关
├── bootstrap/                        # 启动引导
│   └── SchemaBootstrapRunner.java   # 自动生成 Schema
└── graphdb/                          # GraphDB 服务
    ├── GraphDbService.java          # GraphDB 连接
    └── GraphDbImportService.java    # 数据导入
```

## REST API

### GraphDB 管理

| API | 说明 |
|-----|------|
| `GET /api/graphdb/status` | 获取连接状态 |
| `GET /api/graphdb/repositories` | 列出所有仓库 |
| `GET /api/graphdb/repositories/{id}` | 仓库详情 |
| `POST /api/graphdb/repositories/{id}/restart` | 重启仓库 |
| `DELETE /api/graphdb/repositories/{id}` | 删除仓库 |

### SPARQL 查询

| API | 说明 |
|-----|------|
| `POST /api/graphdb/repositories/{id}/sparql` | SELECT 查询 |
| `POST /api/graphdb/repositories/{id}/sparql/construct` | CONSTRUCT 查询 |
| `POST /api/graphdb/repositories/{id}/sparql/ask` | ASK 查询 |

### Schema 生成

| API | 说明 |
|-----|------|
| `POST /api/ontology/generate/owl` | 生成 OWL 本体 |
| `POST /api/ontology/generate/obda` | 生成 OBDA 映射 |
| `POST /api/ontology/generate/all` | 同时生成两者 |

### 文件操作

| API | 说明 |
|-----|------|
| `POST /api/ontology/upload/owl` | 上传 OWL 文件 |
| `POST /api/ontology/upload/obda` | 上传 OBDA 文件 |
| `POST /api/ontology/upload/rdf` | 上传并导入 RDF |
| `GET /api/ontology/files` | 获取文件列表 |

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 2.7.18 |
| OWLAPI | 5.1.9 |
| RDF4J | 3.7.4 |
| Jackson | 2.17.1 |
| MySQL Connector | 8.4.0 |

## 详细文档

查看 `docs/功能与测试手册.md` 获取完整的：
- API 参考
- Java API 使用指南
- 完整测试流程
- GraphDB 对接说明

## License

MIT
