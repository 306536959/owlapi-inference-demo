package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/graphdb")
public class GraphDbController {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbController.class);
    private final SystemBuiltinProperties props;
    private final GraphDbImportService graphDbImportService;

    public GraphDbController(SystemBuiltinProperties props,
                              GraphDbImportService graphDbImportService) {
        this.props = props;
        this.graphDbImportService = graphDbImportService;
    }

    private org.springframework.web.client.RestTemplate createRestTemplate() {
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        String username = props.getGraphDb().getUsername();
        String password = props.getGraphDb().getPassword();
        if (username != null && !username.trim().isEmpty()) {
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().setBasicAuth(username, password == null ? "" : password);
                return execution.execute(request, body);
            });
        }
        return restTemplate;
    }

    private String normalizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        String trimmed = jdbcUrl.trim();
        if (!trimmed.startsWith("jdbc:mysql:")) return trimmed;

        String[] parts = trimmed.split("\\?", 2);
        String base = parts[0];
        Map<String, String> query = new HashMap<>();
        if (parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) {
            for (String pair : parts[1].split("&")) {
                if (pair.isEmpty()) continue;
                String[] kv = pair.split("=", 2);
                query.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        query.put("connectTimeout", "600000");
        query.put("socketTimeout", "600000");
        query.put("tcpKeepAlive", "true");
        query.put("allowPublicKeyRetrieval", "true");
        query.put("useSSL", "false");
        query.put("serverTimezone", "Asia/Shanghai");

        StringBuilder sb = new StringBuilder(base).append("?");
        boolean first = true;
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (!first) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    // ========== Repository Management APIs ==========

    /**
     * Get GraphDB server status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<String> resp = restTemplate.getForEntity(graphDbUrl + "/rest/locations", String.class);
            
            response.put("success", true);
            response.put("connected", resp.getStatusCode().is2xxSuccessful());
            response.put("graphDbUrl", graphDbUrl);
            response.put("status", "online");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking GraphDB status", e);
            response.put("success", false);
            response.put("connected", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * List all repositories
     */
    @GetMapping("/repositories")
    public ResponseEntity<Object> listRepositories() {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String listReposUrl = graphDbUrl + "/rest/repositories";
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(listReposUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error listing repositories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get repository details
     */
    @GetMapping("/repositories/{repoId}")
    public ResponseEntity<Object> getRepositoryDetails(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String repoDetailsUrl = graphDbUrl + "/rest/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(repoDetailsUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting repository details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get repository size (triple count)
     */
    @GetMapping("/repositories/{repoId}/size")
    public ResponseEntity<Object> getRepositorySize(@PathVariable String repoId) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String repoSizeUrl = graphDbUrl + "/rest/repositories/" + repoId + "/size";
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(repoSizeUrl, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error getting repository size", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get repository statistics
     */
    @GetMapping("/repositories/{repoId}/stats")
    public ResponseEntity<Map<String, Object>> getRepositoryStats(@PathVariable String repoId) {
        try {
            Map<String, Object> stats = graphDbImportService.getRepositoryStats(repoId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting repository stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Restart repository
     */
    @PostMapping("/repositories/{repoId}/restart")
    public ResponseEntity<Map<String, Object>> restartRepository(@PathVariable String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String restartUrl = graphDbUrl + "/rest/repositories/" + repoId + "/restart";
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(restartUrl, null, String.class);
            
            if (resp.getStatusCode().is2xxSuccessful()) {
                response.put("success", true);
                response.put("message", "Repository restarted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Failed to restart repository");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error restarting repository", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete repository
     */
    @DeleteMapping("/repositories/{repoId}")
    public ResponseEntity<Map<String, Object>> deleteRepository(@PathVariable String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String deleteUrl = graphDbUrl + "/rest/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            restTemplate.delete(deleteUrl);
            
            response.put("success", true);
            response.put("message", "Repository deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting repository", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Clear repository data
     */
    @DeleteMapping("/repositories/{repoId}/data")
    public ResponseEntity<Map<String, Object>> clearRepository(@PathVariable String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            graphDbImportService.clearRepository(repoId);
            response.put("success", true);
            response.put("message", "Repository data cleared successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing repository", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get repository config files list
     */
    @GetMapping("/repositories/{repoId}/files")
    public ResponseEntity<Map<String, Object>> getRepoFiles(@PathVariable String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/" + repoId + "/";
            File repoDir = new File(uploadDir);
            
            Map<String, Object> files = new HashMap<>();
            
            if (repoDir.exists() && repoDir.isDirectory()) {
                File[] filesInDir = repoDir.listFiles();
                if (filesInDir != null) {
                    for (File f : filesInDir) {
                        String type = getRepoFileType(f.getName());
                        if (type != null) {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", f.getName());
                            fileInfo.put("size", f.length());
                            fileInfo.put("lastModified", f.lastModified());
                            files.put(type, fileInfo);
                        }
                    }
                }
            }
            
            response.put("success", true);
            response.put("files", files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting repo files", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get repository config file content
     */
    @GetMapping("/repositories/{repoId}/files/{fileName}/content")
    public ResponseEntity<Map<String, Object>> getRepoFileContent(@PathVariable String repoId, @PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String filePath = System.getProperty("user.dir") + "/uploads/" + repoId + "/" + fileName;
            File file = new File(filePath);
            
            if (!file.exists()) {
                response.put("success", false);
                response.put("message", "File not found: " + fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("content", content);
            response.put("size", file.length());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error reading repo file", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private String getRepoFileType(String fileName) {
        // Check ttl config file first (more specific)
        if (fileName.endsWith(".ttl") && fileName.contains("config")) {
            return "ttl";
        }
        // Then check standard ontology files
        if (fileName.endsWith(".owl") || fileName.endsWith(".rdf") || fileName.endsWith(".ttl")) {
            return "owl";
        } else if (fileName.endsWith(".obda")) {
            return "obda";
        } else if (fileName.equals("config.properties")) {
            return "properties";
        }
        return null;
    }

    /**
     * Create Ontop virtual repository
     */
    @PostMapping("/repositories/ontop")
    public ResponseEntity<Map<String, Object>> createOntopRepository(
            @RequestParam("id") String id,
            @RequestParam("title") String title,
            @RequestParam("dbUrl") String dbUrl,
            @RequestParam("dbUser") String dbUser,
            @RequestParam("dbPassword") String dbPassword,
            @RequestParam("dbDriver") String dbDriver,
            @RequestParam(value = "owlFile", required = false) MultipartFile owlFile,
            @RequestParam(value = "obdaFile", required = false) MultipartFile obdaFile,
            @RequestParam(value = "generatedOwlFile", required = false) String generatedOwlFile,
            @RequestParam(value = "generatedObdaFile", required = false) String generatedObdaFile,
            @RequestParam("baseIri") String baseIri) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            logger.debug("Creating Ontop virtual repository: {}", id);
            
            String uploadDir = System.getProperty("user.dir") + "/uploads/" + id + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File owlDest = new File(uploadDir + "ontology.owl");
            File obdaDest = new File(uploadDir + "mapping.obda");
            boolean hasUploadFiles = owlFile != null && !owlFile.isEmpty() && obdaFile != null && !obdaFile.isEmpty();
            boolean hasGeneratedFiles = generatedOwlFile != null && !generatedOwlFile.trim().isEmpty()
                    && generatedObdaFile != null && !generatedObdaFile.trim().isEmpty();

            if (hasUploadFiles) {
                owlFile.transferTo(owlDest);
                obdaFile.transferTo(obdaDest);
                logger.debug("Using uploaded OWL/OBDA files");
            } else if (hasGeneratedFiles) {
                File generatedOwl = new File(System.getProperty("user.dir") + "/temp/" + generatedOwlFile);
                File generatedObda = new File(System.getProperty("user.dir") + "/temp/" + generatedObdaFile);
                if (!generatedOwl.exists() || !generatedObda.exists()) {
                    response.put("success", false);
                    response.put("error", "Generated OWL/OBDA files not found in temp directory");
                    return ResponseEntity.badRequest().body(response);
                }
                java.nio.file.Files.copy(generatedOwl.toPath(), owlDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(generatedObda.toPath(), obdaDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Using generated OWL/OBDA files from temp directory");
            } else {
                response.put("success", false);
                response.put("error", "Please upload OWL/OBDA files, or generate them first");
                return ResponseEntity.badRequest().body(response);
            }
            
            File propertiesDest = new File(uploadDir + "config.properties");
            try (java.io.FileWriter writer = new java.io.FileWriter(propertiesDest)) {
                writer.write("jdbc.url=" + normalizeJdbcUrl(dbUrl) + "\n");
                writer.write("jdbc.user=" + dbUser + "\n");
                writer.write("jdbc.password=" + dbPassword + "\n");
                writer.write("jdbc.driver=" + dbDriver + "\n");
                writer.write("base.iri=" + baseIri + "\n");
            }
            
            String graphDbUrl = props.getGraphDb().getUrl();
            String createRepoUrl = graphDbUrl + "/rest/repositories";
            
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
            
            File ttlFile = new File(uploadDir + "repo-config.ttl");
            try (java.io.FileWriter writer = new java.io.FileWriter(ttlFile)) {
                writer.write(ttlConfig);
            }
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            
            org.springframework.core.io.FileSystemResource ttlResource = new org.springframework.core.io.FileSystemResource(ttlFile);
            org.springframework.core.io.FileSystemResource propertiesResource = new org.springframework.core.io.FileSystemResource(propertiesDest);
            org.springframework.core.io.FileSystemResource obdaResource = new org.springframework.core.io.FileSystemResource(obdaDest);
            org.springframework.core.io.FileSystemResource owlResource = new org.springframework.core.io.FileSystemResource(owlDest);
            
            org.springframework.util.MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("config", ttlResource);
            parts.add("propertiesFile", propertiesResource);
            parts.add("obdaFile", obdaResource);
            parts.add("owlFile", owlResource);
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = 
                new org.springframework.http.HttpEntity<>(parts, headers);
            
            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(createRepoUrl, requestEntity, String.class);
            
            if (resp.getStatusCode().is2xxSuccessful()) {
                logger.info("Ontop virtual repository created: {}", id);
                response.put("success", true);
                response.put("message", "Ontop virtual repository created successfully");
                response.put("repositoryId", id);
            } else {
                response.put("success", false);
                response.put("message", "Failed: " + resp.getStatusCode().getReasonPhrase());
            }
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error saving files", e);
            response.put("success", false);
            response.put("error", "Error saving files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("Error creating Ontop repository", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create FedX federation repository
     */
    @PostMapping("/repositories/fedx")
    public ResponseEntity<Map<String, Object>> createFedxRepository(
            @RequestParam("id") String id,
            @RequestParam("title") String title,
            @RequestParam("members") String members) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (id == null || id.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Repository id is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (members == null || members.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "At least one federation member is required");
                return ResponseEntity.badRequest().body(response);
            }

            String[] memberIds = java.util.Arrays.stream(members.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (memberIds.length == 0) {
                response.put("success", false);
                response.put("error", "At least one federation member is required");
                return ResponseEntity.badRequest().body(response);
            }

            String createRepoUrl = props.getGraphDb().getUrl() + "/rest/repositories";
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();

            java.util.List<Map<String, String>> memberValues = new java.util.ArrayList<>();
            for (String memberId : memberIds) {
                Map<String, String> member = new HashMap<>();
                member.put("repoType", "ontop");
                member.put("repositoryName", memberId);
                member.put("respectRights", "true");
                member.put("store", "ResolvableRepository");
                member.put("writable", "false");
                memberValues.add(member);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("title", title);
            payload.put("type", "fedx");
            payload.put("sesameType", "graphdb:FedXRepository");

            Map<String, Object> params = new HashMap<>();
            params.put("queryTimeout", mapParam("queryTimeout", "Query timeout (seconds)", "0"));
            params.put("includeInferredDefault", mapParam("includeInferredDefault", "Include inferred default", "true"));
            params.put("member", mapParam("member", "FedX repo members", memberValues));
            params.put("enableServiceAsBoundJoin", mapParam("enableServiceAsBoundJoin", "Enable service as bound join", "true"));
            params.put("boundJoinBlockSize", mapParam("boundJoinBlockSize", "Bound join block size", "15"));
            params.put("joinWorkerThreads", mapParam("joinWorkerThreads", "Join worker threads", "20"));
            params.put("unionWorkerThreads", mapParam("unionWorkerThreads", "Union worker threads", "20"));
            params.put("leftJoinWorkerThreads", mapParam("leftJoinWorkerThreads", "Left join worker threads", "10"));
            params.put("sourceCacheSpec", mapParam("sourceCacheSpec", "Source selection cache spec", "maximumSize=1000,expireAfterWrite=6h"));
            params.put("enableMonitoring", mapParam("enableMonitoring", "Enable monitoring", "false"));
            params.put("isLogQueries", mapParam("isLogQueries", "Log queries", "false"));
            params.put("isLogQueryPlan", mapParam("isLogQueryPlan", "Log query plan", "false"));
            params.put("debugQueryPlan", mapParam("debugQueryPlan", "Debug query plan", "false"));
            payload.put("params", params);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity =
                    new org.springframework.http.HttpEntity<>(payload, headers);

            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(createRepoUrl, requestEntity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                response.put("success", true);
                response.put("message", "FedX repository created successfully");
                response.put("repositoryId", id);
                response.put("members", memberIds);
            } else {
                response.put("success", false);
                response.put("message", "Failed: " + resp.getStatusCode().getReasonPhrase());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating FedX repository", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Map<String, Object> mapParam(String name, String label, Object value) {
        Map<String, Object> param = new HashMap<>();
        param.put("name", name);
        param.put("label", label);
        param.put("value", value);
        return param;
    }

    // ========== SPARQL Query APIs ==========

    /**
     * Execute SPARQL SELECT query (GET, for browser convenience)
     */
    @GetMapping("/repositories/{repoId}/sparql")
    public ResponseEntity<Object> executeSparqlQueryGet(@PathVariable String repoId,
                                                        @RequestParam String query) {
        try {
            if (query == null || query.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Query is required"));
            }
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;

            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("query", query);
            body.add("format", "application/sparql-results+json");
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<Object> response = restTemplate.postForEntity(sparqlUrl, requestEntity, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error executing SPARQL GET query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Execute SPARQL SELECT query
     */
    @PostMapping("/repositories/{repoId}/sparql")
    public ResponseEntity<Object> executeSparqlQuery(@PathVariable String repoId, 
                                                      @RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            if (query == null || query.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Query parameter is required"));
            }
            
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            
            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("query", query);
            body.add("format", "application/sparql-results+json");
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity = 
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<Object> response = restTemplate.postForEntity(sparqlUrl, requestEntity, Object.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error executing SPARQL query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Execute SPARQL CONSTRUCT query
     */
    @PostMapping("/repositories/{repoId}/sparql/construct")
    public ResponseEntity<Map<String, Object>> executeSparqlConstruct(@PathVariable String repoId, 
                                                                       @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = request.get("query");
            if (query == null || query.isEmpty()) {
                response.put("success", false);
                response.put("error", "Query parameter is required");
                return ResponseEntity.badRequest().body(response);
            }
            String format = request.getOrDefault("format", "turtle");
            
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", format.equals("jsonld") ? "application/ld+json" : 
                              format.equals("nt") ? "application/n-triples" : "application/x-turtle");
            
            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("query", query);
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity = 
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(sparqlUrl, requestEntity, String.class);
            
            response.put("success", true);
            response.put("data", resp.getBody());
            response.put("format", format);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error executing SPARQL CONSTRUCT", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Execute SPARQL ASK query
     */
    @PostMapping("/repositories/{repoId}/sparql/ask")
    public ResponseEntity<Map<String, Object>> executeSparqlAsk(@PathVariable String repoId, 
                                                                 @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = request.get("query");
            if (query == null || query.isEmpty()) {
                response.put("success", false);
                response.put("error", "Query parameter is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/sparql-results+json");
            
            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("query", query);
            
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity = 
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(sparqlUrl, requestEntity, String.class);
            
            response.put("success", true);
            response.put("result", resp.getBody());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error executing SPARQL ASK", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== Saved Queries APIs ==========

    /**
     * List all saved queries
     */
    @GetMapping("/saved-queries")
    public ResponseEntity<Object> listSavedQueries(@RequestParam(required = false) String name) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String url = graphDbUrl + "/rest/sparql/saved-queries" + (name != null ? "?name=" + name : "");
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            
            // GraphDB returns { "value": [...], "Count": N } - extract "value"
            Object body = response.getBody();
            if (body instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) body;
                if (map.containsKey("value")) {
                    return ResponseEntity.ok(map.get("value"));
                }
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            logger.error("Error listing saved queries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Create a new saved query
     */
    @PostMapping("/saved-queries")
    public ResponseEntity<Map<String, Object>> createSavedQuery(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String url = graphDbUrl + "/rest/sparql/saved-queries";
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<Map<String, String>> requestEntity = 
                new org.springframework.http.HttpEntity<>(request, headers);
            
            org.springframework.http.ResponseEntity<String> resp = restTemplate.postForEntity(url, requestEntity, String.class);
            
            if (resp.getStatusCode().is2xxSuccessful()) {
                response.put("success", true);
                response.put("message", "Saved query created successfully");
            } else {
                response.put("success", false);
                response.put("message", resp.getStatusCode().getReasonPhrase());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating saved query", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update an existing saved query.
     * oldQueryName must be a URL query parameter for GraphDB; encoded once with URLEncoder (not UriComponentsBuilder).
     */
    @PutMapping("/saved-queries")
    public ResponseEntity<Map<String, Object>> updateSavedQuery(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String oldQueryName = request.get("oldQueryName");
            if (oldQueryName == null || oldQueryName.isEmpty()) {
                oldQueryName = request.get("name");
            }
            if (oldQueryName == null || oldQueryName.isEmpty()) {
                response.put("success", false);
                response.put("error", "oldQueryName is required for update");
                return ResponseEntity.badRequest().body(response);
            }
            // Encode exactly once (spaces -> %20), not via UriComponentsBuilder which double-encodes
            String encodedName = java.net.URLEncoder.encode(oldQueryName, java.nio.charset.StandardCharsets.UTF_8.toString());
            String url = graphDbUrl + "/rest/sparql/saved-queries?oldQueryName=" + encodedName;

            Map<String, String> graphDbRequest = new HashMap<>();
            graphDbRequest.put("name", request.get("name"));
            graphDbRequest.put("body", request.get("body"));

            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, String>> requestEntity =
                    new org.springframework.http.HttpEntity<>(graphDbRequest, headers);

            restTemplate.put(url, requestEntity);

            response.put("success", true);
            response.put("message", "Saved query updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating saved query", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a saved query
     */
    @DeleteMapping("/saved-queries")
    public ResponseEntity<Map<String, Object>> deleteSavedQuery(@RequestParam String name) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String url = graphDbUrl + "/rest/sparql/saved-queries?name=" + name;
            
            org.springframework.web.client.RestTemplate restTemplate = createRestTemplate();
            restTemplate.delete(url);
            
            response.put("success", true);
            response.put("message", "Saved query deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting saved query", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
