package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbImportService;
import com.example.owlapi.graphdb.GraphDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/graphdb")
public class GraphDbController {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbController.class);
    private final SystemBuiltinProperties props;
    private final GraphDbImportService graphDbImportService;
    private final GraphDbService graphDbService;

    public GraphDbController(SystemBuiltinProperties props, 
                              GraphDbImportService graphDbImportService,
                              GraphDbService graphDbService) {
        this.props = props;
        this.graphDbImportService = graphDbImportService;
        this.graphDbService = graphDbService;
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
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
            @RequestParam("owlFile") MultipartFile owlFile,
            @RequestParam("obdaFile") MultipartFile obdaFile,
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
            owlFile.transferTo(owlDest);
            logger.debug("OWL file saved: {}", owlDest.getAbsolutePath());
            
            File obdaDest = new File(uploadDir + "mapping.obda");
            obdaFile.transferTo(obdaDest);
            
            File propertiesDest = new File(uploadDir + "config.properties");
            try (java.io.FileWriter writer = new java.io.FileWriter(propertiesDest)) {
                writer.write("jdbc.url=" + dbUrl + "\n");
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
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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

    // ========== SPARQL Query APIs ==========

    /**
     * Execute SPARQL SELECT query
     */
    @PostMapping("/repositories/{repoId}/sparql")
    public ResponseEntity<Object> executeSparqlQuery(@PathVariable String repoId, 
                                                      @RequestParam("query") String query,
                                                      @RequestParam(value = "format", defaultValue = "json") String format) {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
                                                                       @RequestParam("query") String query,
                                                                       @RequestParam(value = "format", defaultValue = "turtle") String format) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
                                                                  @RequestParam("query") String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String sparqlUrl = graphDbUrl + "/repositories/" + repoId;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
}
