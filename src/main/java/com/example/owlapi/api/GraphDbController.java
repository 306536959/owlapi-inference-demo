package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/graphdb")
public class GraphDbController {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbController.class);
    private final SystemBuiltinProperties props;

    public GraphDbController(SystemBuiltinProperties props) {
        this.props = props;
    }

    // 新建 Ontop 虚拟库
    @PostMapping("/repositories/ontop")
    public ResponseEntity<String> createOntopRepository(@RequestParam("id") String id,
                                                      @RequestParam("title") String title,
                                                      // 数据库连接配置
                                                      @RequestParam("dbUrl") String dbUrl,
                                                      @RequestParam("dbUser") String dbUser,
                                                      @RequestParam("dbPassword") String dbPassword,
                                                      @RequestParam("dbDriver") String dbDriver,
                                                      // Ontop 相关配置
                                                      @RequestParam("owlFile") MultipartFile owlFile,
                                                      @RequestParam("obdaFile") MultipartFile obdaFile,
                                                      @RequestParam("baseIri") String baseIri) {
        try {
            logger.debug("Creating Ontop virtual repository: {}", id);
            
            // 1. 保存上传的文件
            String uploadDir = System.getProperty("user.dir") + "/uploads/" + id + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存 OWL 文件
            File owlDest = new File(uploadDir + "ontology.owl");
            owlFile.transferTo(owlDest);
            
            // 保存 OBDA 文件
            File obdaDest = new File(uploadDir + "mapping.obda");
            obdaFile.transferTo(obdaDest);
            
            // 创建数据库连接配置文件
            File propertiesDest = new File(uploadDir + "config.properties");
            try (java.io.FileWriter writer = new java.io.FileWriter(propertiesDest)) {
                writer.write("jdbc.url=" + dbUrl + "\n");
                writer.write("jdbc.user=" + dbUser + "\n");
                writer.write("jdbc.password=" + dbPassword + "\n");
                writer.write("jdbc.driver=" + dbDriver + "\n");
                writer.write("base.iri=" + baseIri + "\n");
            }
            
            // 2. 配置 Ontop 虚拟库
            // 调用 GraphDB 的 REST API 创建 Ontop 虚拟库
            try {
                String graphDbUrl = props.getGraphDb().getUrl();
                String createRepoUrl = graphDbUrl + "/rest/repositories";
                
                // 创建 TTL 配置文件
                String ttlConfig = String.format(
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                    "@prefix rep: <http://www.openrdf.org/config/repository#> .\n" +
                    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n" +
                    "<#%s> a rep:Repository;\n" +
                    "  rep:repositoryID \"%s\";\n" +
                    "  rep:repositoryImpl [\n" +
                    "      <http://inf.unibz.it/krdb/obda/quest#propertiesFile> \"%s\";\n" +
                    "      <http://inf.unibz.it/krdb/obda/quest#obdaFile> \"%s\";\n" +
                    "      <http://inf.unibz.it/krdb/obda/quest#owlFile> \"%s\";\n" +
                    "      rep:repositoryType \"graphdb:OntopRepository\"\n" +
                    "    ];\n" +
                    "  rdfs:label \"%s\" .",
                    id, id, propertiesDest.getName(), obdaDest.getName(), owlDest.getName(), title
                );
                
                // 保存 TTL 配置文件
                File ttlFile = new File(uploadDir + "repo-config.ttl");
                try (java.io.FileWriter writer = new java.io.FileWriter(ttlFile)) {
                    writer.write(ttlConfig);
                }
                
                // 创建 RestTemplate
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                
                // 创建多部分请求
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
                
                // 创建文件资源
                org.springframework.core.io.FileSystemResource ttlResource = new org.springframework.core.io.FileSystemResource(ttlFile);
                org.springframework.core.io.FileSystemResource propertiesResource = new org.springframework.core.io.FileSystemResource(propertiesDest);
                org.springframework.core.io.FileSystemResource obdaResource = new org.springframework.core.io.FileSystemResource(obdaDest);
                org.springframework.core.io.FileSystemResource owlResource = new org.springframework.core.io.FileSystemResource(owlDest);
                
                // 创建多部分请求实体
                org.springframework.util.MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
                parts.add("config", ttlResource);
                parts.add("propertiesFile", propertiesResource);
                parts.add("obdaFile", obdaResource);
                parts.add("owlFile", owlResource);
                
                org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(parts, headers);
                
                // 执行请求
                org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(createRepoUrl, requestEntity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Ontop virtual repository created successfully: {}", id);
                    return ResponseEntity.ok("Ontop virtual repository created successfully");
                } else {
                    logger.error("Failed to create Ontop virtual repository, status code: {}", response.getStatusCode());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create Ontop virtual repository: " + response.getStatusCode().getReasonPhrase());
                }
            } catch (Exception e) {
                logger.error("Error creating Ontop virtual repository via GraphDB API", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Ontop virtual repository: " + e.getMessage());
            }
        } catch (IOException e) {
            logger.error("Error saving uploaded files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving uploaded files: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Ontop virtual repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Ontop virtual repository: " + e.getMessage());
        }
    }

    // 列出所有仓库
    @GetMapping("/repositories")
    public ResponseEntity<Object> listRepositories() {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String listReposUrl = graphDbUrl + "/rest/repositories";
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(listReposUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error listing repositories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error listing repositories: " + e.getMessage());
        }
    }

    // 查看单个仓库详情
    @GetMapping("/repositories/{repoId}")
    public ResponseEntity<Object> getRepositoryDetails(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String repoDetailsUrl = graphDbUrl + "/rest/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(repoDetailsUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting repository details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting repository details: " + e.getMessage());
        }
    }

    // 获取仓库三元组数量
    @GetMapping("/repositories/{repoId}/size")
    public ResponseEntity<Object> getRepositorySize(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String repoSizeUrl = graphDbUrl + "/rest/repositories/" + repoId + "/size";
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(repoSizeUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting repository size", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting repository size: " + e.getMessage());
        }
    }

    // 重启仓库
    @PostMapping("/repositories/{repoId}/restart")
    public ResponseEntity<String> restartRepository(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String restartUrl = graphDbUrl + "/rest/repositories/" + repoId + "/restart";
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(restartUrl, null, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Repository restarted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to restart repository: " + response.getStatusCode().getReasonPhrase());
            }
        } catch (Exception e) {
            logger.error("Error restarting repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error restarting repository: " + e.getMessage());
        }
    }

    // 删除仓库
    @DeleteMapping("/repositories/{repoId}")
    public ResponseEntity<String> deleteRepository(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String deleteUrl = graphDbUrl + "/rest/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            restTemplate.delete(deleteUrl);
            
            return ResponseEntity.ok("Repository deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting repository: " + e.getMessage());
        }
    }

    // 编辑仓库配置
    @PutMapping("/repositories/{repoId}")
    public ResponseEntity<String> updateRepository(@PathVariable String repoId, @RequestBody Object config) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String updateUrl = graphDbUrl + "/rest/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                updateUrl,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(config),
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Repository updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update repository: " + response.getStatusCode().getReasonPhrase());
            }
        } catch (Exception e) {
            logger.error("Error updating repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating repository: " + e.getMessage());
        }
    }

    // 执行 SPARQL 查询
    @PostMapping("/repositories/{repoId}/sparql")
    public ResponseEntity<Object> executeSparqlQuery(@PathVariable String repoId, @RequestParam("query") String query) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            
            // 创建请求实体
            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("query", query);
            body.add("format", "application/sparql-results+json");
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity = new org.springframework.http.HttpEntity<>(body, headers);
            
            // 执行请求
            org.springframework.http.ResponseEntity<Object> response = restTemplate.postForEntity(sparqlUrl, requestEntity, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error executing SPARQL query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing SPARQL query: " + e.getMessage());
        }
    }
}
