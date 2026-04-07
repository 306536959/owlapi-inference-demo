# GraphDB REST API 学习手册

> 整理日期：2026-04-03
> 文档目的：梳理项目中已实现和未实现的 GraphDB REST API

---

## 一、项目已实现的 API

| API | 方法 | 端点 | 功能说明 |
|-----|------|------|----------|
| 服务器状态 | GET | `/api/graphdb/status` | 检查 GraphDB 连接状态 |
| 列出仓库 | GET | `/api/graphdb/repositories` | 获取所有仓库列表 |
| 仓库详情 | GET | `/api/graphdb/repositories/{repoId}` | 获取单个仓库详细信息 |
| 仓库大小 | GET | `/api/graphdb/repositories/{repoId}/size` | 获取仓库三元组数量 |
| 仓库统计 | GET | `/api/graphdb/repositories/{repoId}/stats` | 获取仓库各类统计信息 |
| 重启仓库 | POST | `/api/graphdb/repositories/{repoId}/restart` | 重启指定仓库 |
| 删除仓库 | DELETE | `/api/graphdb/repositories/{repoId}` | 删除指定仓库 |
| 清空数据 | DELETE | `/api/graphdb/repositories/{repoId}/data` | 清空仓库所有数据 |
| 查看文件 | GET | `/api/graphdb/repositories/{repoId}/files` | 查看仓库配置文件列表 |
| 文件内容 | GET | `/api/graphdb/repositories/{repoId}/files/{fileName}/content` | 读取配置文件内容 |
| 创建Ontop仓库 | POST | `/api/graphdb/repositories/ontop` | 创建 Ontop 虚拟仓库 |
| SPARQL查询 | POST | `/api/graphdb/repositories/{repoId}/sparql` | 执行 SELECT 查询 |
| SPARQL构造 | POST | `/api/graphdb/repositories/{repoId}/sparql/construct` | 执行 CONSTRUCT 查询 |
| SPARQL询问 | POST | `/api/graphdb/repositories/{repoId}/sparql/ask` | 执行 ASK 查询 |
| **列出保存的查询** | GET | `/api/graphdb/saved-queries` | 列出所有保存的查询 |
| **创建保存的查询** | POST | `/api/graphdb/saved-queries` | 创建新的保存查询 |
| **更新保存的查询** | PUT | `/api/graphdb/saved-queries` | 更新保存的查询 |
| **删除保存的查询** | DELETE | `/api/graphdb/saved-queries` | 删除保存的查询 |

---

## 二、项目未实现的 API（可集成）

### 2.1 数据导入 (Data Import)

**功能说明**：从 URL 或服务器导入 RDF 数据到仓库

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 从URL导入 | POST | `/rest/repositories/{repoId}/import/rdf/{source}` | `curl -X POST <base>/rest/repositories/repo1/import/rdf/http?file=https://example.com/data.ttl` |
| 从服务器导入 | POST | `/rest/repositories/{repoId}/import/rdf/files` | `curl -X POST -F "file=@data.ttl" <base>/rest/repositories/repo1/import/rdf/files` |

**参数说明**：
- `source` 可以是 `url`、`server` 或 `upload`
- `policy` 可选：`SKIP`, `REPLACE`, `ADD`

**适用场景**：
- 批量导入外部 RDF 数据
- 从远程服务器同步数据

---

### 2.2 保存的查询 (Saved Queries) ✅ 已实现

**功能说明**：保存和管理常用的 SPARQL 查询

**前端页面**: `saved-queries.html` 或点击主页左侧导航「保存的查询」

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 列出查询 | GET | `/api/graphdb/saved-queries` | 已实现 |
| 获取单个 | GET | `/api/graphdb/saved-queries?name={name}` | 已实现 |
| 创建查询 | POST | `/api/graphdb/saved-queries` | 已实现 |
| 更新查询 | PUT | `/api/graphdb/saved-queries` | 已实现 |
| 删除查询 | DELETE | `/api/graphdb/saved-queries?name={name}` | 已实现 |

**创建/更新请求体**：
```json
{
    "name": "query_name",
    "body": "SELECT * WHERE { ?s ?p ?o } LIMIT 10",
    "description": "查询描述（可选）"
}
```

**适用场景**：
- 频繁使用的固定查询
- 团队共享查询模板

---

### 2.3 SPARQL 模板 (SPARQL Templates)

**功能说明**：带参数的 SPARQL 查询模板，可动态执行

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 列出模板 | GET | `/rest/repositories/{repoId}/sparql-templates` | `curl <base>/rest/repositories/repo1/sparql-templates` |
| 模板配置 | GET | `/rest/repositories/{repoId}/sparql-templates/configuration?templateID={id}` | 获取模板配置 |
| 创建模板 | POST | `/rest/repositories/{repoId}/sparql-templates` | 见下方示例 |
| 更新模板 | PUT | `/rest/repositories/{repoId}/sparql-templates?templateID={id}` | 更新模板内容 |
| 删除模板 | DELETE | `/rest/repositories/{repoId}/sparql-templates?templateID={id}` | 删除模板 |
| 执行模板 | POST | `/rest/repositories/{repoId}/sparql-templates/execute` | 带参数执行 |

**创建模板请求体**：
```json
{
    "templateID": "wineTemplate",
    "query": "PREFIX wine: <http://www.example.com/wine#>\nSELECT ?wine WHERE { ?wine wine:hasSugar ?sugar FILTER(?sugar = '${sugar}') }"
}
```

**执行模板请求体**：
```json
{
    "sugar": "none",
    "year": 2020,
    "s": "http://www.example.com/wine#Blanquito"
}
```

**适用场景**：
- 需要参数化的复杂查询
- 构建可复用的查询服务

---

### 2.4 SQL 视图 (SQL Views)

**功能说明**：用 SQL 语法定义虚拟表，查询时映射为 SPARQL

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 列出视图 | GET | `/rest/sql-views/tables` | `curl -H "X-GraphDB-Repository: repo1" <base>/rest/sql-views/tables` |
| 视图详情 | GET | `/rest/sql-views/tables/{name}` | 获取视图定义 |
| 创建视图 | POST | `/rest/sql-views/tables/` | 见下方示例 |
| 更新视图 | PUT | `/rest/sql-views/tables/{name}` | 更新视图 |
| 删除视图 | DELETE | `/rest/sql-views/tables/{name}` | 删除视图 |

**创建视图请求体**：
```json
{
    "name": "PersonView",
    "query": "SELECT id, name, email FROM users",
    "columns": ["id", "name", "email"]
}
```

**适用场景**：
- 习惯 SQL 的开发者
- 快速映射关系数据库到 RDF

---

### 2.5 仓库管理扩展 (Repository Management)

**功能说明**：完整的仓库 CRUD 操作

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 创建普通仓库 | POST | `/rest/repositories` | `curl -X POST -F "config=@repo.ttl" <base>/rest/repositories` |
| 更新仓库配置 | PUT | `/rest/repositories/{repoId}` | 发送完整配置 JSON |

**创建普通（非Ontop）仓库的 TTL 示例**：
```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix graphdb: <http://www.ontotext.com/config/graphdb#>.

[] a rep:Repository ;
  rep:repositoryID "my-repo" ;
  rdfs:label "My Repository" ;
  rep:repositoryImpl [
    rep:repositoryType "graphdb:SailRepository" ;
    sr:sailImpl [
      sail:sailType "graphdb:Sail" ;
      graphdb:ruleset "owl-horst-optimized"
    ]
  ].
```

**仓库可配置参数**：
| 参数 | 说明 | 可选值 |
|------|------|--------|
| ruleset | 推理规则集 | `empty`, `rdfs`, `owl-horst-optimized`, `rdfs-plus-optimized`, `owl-max-optimized` |
| enableFtsIndex | 全文搜索 | `true/false` |
| disableSameAs | 禁用 owl:sameAs | `true/false` |
| validationEnabled | SHACL验证 | `true/false` |
| queryTimeout | 查询超时(秒) | 数字 |
| readOnly | 只读模式 | `true/false` |

---

### 2.6 位置管理 (Location Management)

**功能说明**：连接和管理远程 GraphDB 实例

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 列出位置 | GET | `/rest/locations` | `curl <base>/rest/locations` |
| 添加位置 | POST | `/rest/locations` | 添加新位置 |
| 连接远程 | PUT | `/rest/locations` | 连接远程实例 |
| 断开位置 | DELETE | `/rest/locations?uri={encoded_uri}` | 断开连接 |
| 设置默认 | POST | `/rest/locations/active/default-repository` | 设置默认仓库 |

**连接远程 GraphDB**：
```json
{
    "username": "admin",
    "password": "root",
    "uri": "http://192.168.1.100:7200/"
}
```

---

### 2.7 SHACL 验证 (SHACL Validation)

**功能说明**：验证 RDF 数据是否符合 SHACL 形状约束

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 验证(用仓库存形状) | POST | `/rest/repositories/{dataRepo}/validate/repository/{shapesRepo}` | 跨仓库验证 |
| 验证(直接发送形状) | POST | `/rest/repositories/{repoId}/validate/text` | 发送 SHACL 形状 |

**验证请求示例**：
```bash
curl -X POST -H "Content-Type: text/turtle" \
    --data-raw "@prefix ex: <http://example.org/ns#> .
                @prefix sh: <http://www.w3.org/ns/shacl#> .
                ex:PersonShape a sh:NodeShape ;
                    sh:targetClass ex:Person ;
                    sh:property [ sh:path ex:age ; sh:datatype xsd:integer ] ." \
    http://localhost:7200/rest/repositories/my-repo/validate/text
```

**适用场景**：
- 数据质量检查
- 数据导入前验证

---

### 2.8 安全与用户管理 (Security & Users)

**功能说明**：管理用户账号和访问权限

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 安全状态 | GET | `/rest/security` | 检查安全是否启用 |
| 启用安全 | POST | `/rest/security` | `curl -X POST -d true <base>/rest/security` |
| 免费访问 | GET | `/rest/security/free-access` | 检查自由访问状态 |
| 列出用户 | GET | `/rest/security/users` | 获取所有用户 |
| 获取用户 | GET | `/rest/security/users/{username}` | 获取单个用户信息 |
| 创建用户 | POST | `/rest/security/users/{username}` | 创建新用户 |
| 更新用户 | PUT | `/rest/security/users/{username}` | 更新用户 |
| 删除用户 | DELETE | `/rest/security/users/{username}` | 删除用户 |

**创建用户请求体**：
```json
{
    "username": "newuser",
    "password": "password123",
    "grantedAuthorities": ["ROLE_USER"],
    "appSettings": {
        "DEFAULT_INFERENCE": true,
        "DEFAULT_SAMEAS": false,
        "EXECUTE_COUNT": true
    }
}
```

**权限角色**：
- `ROLE_USER` - 普通用户
- `ROLE_ADMIN` - 管理员

---

### 2.9 身份验证 (Authentication)

**功能说明**：获取访问令牌（Token）

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 获取Token | POST | `/rest/login/{username}` | `curl -X POST -H "X-GraphDB-Password: pass" <base>/rest/login/admin` |

**返回信息**：
```json
{
    "authorities": ["ROLE_USER"],
    "gdbToken": "eyJhbGciOiJIUzI1NiJ9...",
    "appSettings": {...}
}
```

**后续请求使用 Token**：
```
Authorization: GDB <gdbToken>
```

---

### 2.10 监控 (Monitoring)

**功能说明**：系统运行指标，支持 Prometheus 采集

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 仓库监控 | GET | `/rest/monitor/repository` | 获取仓库监控数据 |
| 结构统计 | GET | `/rest/monitor/structures` | 获取结构统计数据 |
| 基础设施 | GET | `/rest/monitor/infrastructure` | CPU/内存/磁盘统计 |

**Prometheus 集成端点**：
- `/rest/monitor/structures`
- `/rest/monitor/repository`
- `/rest/monitor/infrastructure`
- `/rest/monitor/cluster`
- `/rest/monitor/backup`

---

### 2.11 集群管理 (Cluster) ⚠️ 需要企业版

**功能说明**：多节点集群管理

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 获取集群配置 | GET | `/rest/cluster/config` | 获取集群信息 |
| 集群状态 | GET | `/rest/cluster/group/status` | 获取集群状态 |
| 创建集群 | POST | `/rest/cluster/config` | 创建集群 |
| 添加节点 | POST | `/rest/cluster/config/node` | 添加集群节点 |
| 删除集群 | DELETE | `/rest/cluster/config` | 删除集群 |
| 集群监控 | GET | `/rest/monitor/cluster` | 获取集群监控 |

---

### 2.12 备份与恢复 (Backup & Recovery)

**功能说明**：备份和恢复 GraphDB 数据

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 创建备份 | POST | `/rest/repositories/{repoId}/backup` | 创建备份 |
| 恢复备份 | POST | `/rest/repositories/{repoId}/restore` | 从备份恢复 |
| 备份监控 | GET | `/rest/monitor/backup` | 查看备份进度 |

---

### 2.13 命名空间管理 (Namespaces) - RDF4J API

**功能说明**：管理 RDF 命名空间前缀

| 操作 | 方法 | 端点 | curl 示例 |
|------|------|------|-----------|
| 列出命名空间 | GET | `/repositories/{repoId}/namespaces` | 获取所有前缀 |
| 获取前缀 | GET | `/repositories/{repoId}/namespaces/{prefix}` | 获取命名空间 URI |
| 创建前缀 | POST | `/repositories/{repoId}/namespaces` | 添加新前缀 |
| 删除前缀 | DELETE | `/repositories/{repoId}/namespaces/{prefix}` | 删除前缀 |

**创建命名空间示例**：
```
POST /repositories/repo1/namespaces
Content-Type: text/plain

ex
http://example.org/
```

---

### 2.14 事务管理 (Transactions) - RDF4J API

**功能说明**：显式事务控制

| 操作 | 方法 | 端点 | 说明 |
|------|------|------|------|
| 开始事务 | POST | `/repositories/{repoId}/transactions` | 返回 transaction ID |
| 提交事务 | POST | `/repositories/{repoId}/transactions/{txId}` | 提交 |
| 回滚事务 | DELETE | `/repositories/{repoId}/transactions/{txId}` | 回滚 |

---

## 三、功能对比与建议

### 已实现 vs 未实现

| 类别 | 已实现 | 未实现 |
|------|--------|--------|
| **查询** | SELECT, CONSTRUCT, ASK | - |
| **仓库管理** | 创建Ontop, 删除, 查看, 重启 | 创建普通仓库, 更新配置 |
| **数据操作** | 清空数据 | 导入数据, 导出数据 |
| **监控** | 基本统计 | Prometheus, 基础设施监控 |
| **安全** | - | 用户管理, 权限控制 |
| **高级** | **Saved Queries** | Templates, SQL Views, SHACL, 集群, 备份 |

### 学习优先级建议

| 优先级 | 功能 | 状态 | 理由 |
|--------|------|------|------|
| ⭐⭐⭐ | **数据导入** | 待实现 | 实用性强，使用频率高 |
| ⭐⭐⭐ | **SHACL 验证** | 待实现 | 数据质量保障 |
| ⭐⭐ | ~~**Saved Queries**~~ | ✅ 已实现 | 提高工作效率 |
| ⭐⭐ | **仓库配置更新** | 待实现 | 完善仓库管理能力 |
| ⭐ | **监控** | 待实现 | 可选，非核心功能 |
| ⭐ | **用户管理** | 待实现 | 安全相关，需谨慎 |
| ❌ | **集群管理** | 不推荐 | 需要企业版许可证 |

---

## 四、参考文档

- [GraphDB REST API 文档](https://graphdb.ontotext.com/documentation/11.3/using-the-graphdb-rest-api.html)
- [GraphDB 管理 API (curl)](https://graphdb.ontotext.com/documentation/11.3/admin-with-curl.html)
- [GraphDB 仓库管理](https://graphdb.ontotext.com/documentation/11.3/manage-repos-with-restapi.html)
- [RDF4J REST API](https://rdf4j.org/documentation/reference/rest-api/)
- [RDF4J Javadoc](https://rdf4j.org/javadoc/latest/)

---

## 五、后续学习计划

1. **第一阶段**：数据导入 + SHACL 验证
2. **第二阶段**：Saved Queries + 仓库配置更新
3. **第三阶段**：监控 + 用户管理
