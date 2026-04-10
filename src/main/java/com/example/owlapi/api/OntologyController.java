package com.example.owlapi.api;

import com.example.owlapi.SchemaGenerationResult;
import com.example.owlapi.SchemaGeneratorArgs;
import com.example.owlapi.SchemaToOwlGenerator;
import com.example.owlapi.ObdaGeneratorArgs;
import com.example.owlapi.SchemaToObdaGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/ontology")
public class OntologyController {

    private static final Logger logger = LoggerFactory.getLogger(OntologyController.class);

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
        // Force override to avoid existing small timeout values (e.g. 20000ms)
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

    // ========== Schema Generation APIs ==========

    @PostMapping(value = "/datasource/test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> testDataSource(@RequestBody SchemaGenRequest request) {
        Map<String, Object> response = new HashMap<>();
        long start = System.currentTimeMillis();
        try {
            if (request.jdbcUrl == null || request.jdbcUrl.trim().isEmpty()
                    || request.user == null || request.user.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "jdbcUrl and user are required");
                return ResponseEntity.badRequest().body(response);
            }

            String driverClass = request.driverClass != null && !request.driverClass.trim().isEmpty()
                    ? request.driverClass
                    : "com.mysql.cj.jdbc.Driver";
            Class.forName(driverClass);
            String jdbcUrl = normalizeJdbcUrl(request.jdbcUrl);
            try (Connection conn = DriverManager.getConnection(jdbcUrl, request.user, request.password == null ? "" : request.password)) {
                response.put("success", true);
                response.put("message", "数据库连接成功");
                response.put("valid", conn.isValid(3));
                response.put("jdbcUrl", jdbcUrl);
                response.put("elapsedMs", System.currentTimeMillis() - start);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error testing data source connection", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("elapsedMs", System.currentTimeMillis() - start);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Generate OWL ontology from database schema
     */
    @PostMapping(value = "/generate/owl", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateOwl(@RequestBody SchemaGenRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Generating OWL ontology from database schema...");

            // Create temp directory
            String workDir = System.getProperty("user.dir");
            File tempDir = new File(workDir + "/temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Generate unique filename with timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String outputFile = "schema_" + timestamp + ".owl";
            
            SchemaGeneratorArgs args = new SchemaGeneratorArgs();
            args.jdbcUrl = normalizeJdbcUrl(request.jdbcUrl);
            args.user = request.user;
            args.password = request.password;
            args.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            args.catalog = request.catalog;
            args.schema = request.schema;
            args.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            args.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            args.output = "temp/" + outputFile;
            
            if (request.includeTables != null) {
                args.includeTables = Stream.of(request.includeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }
            if (request.excludeTables != null) {
                args.excludeTables = Stream.of(request.excludeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }

            SchemaGenerationResult result = SchemaToOwlGenerator.generate(args);

            response.put("success", true);
            response.put("message", "OWL ontology generated successfully");
            response.put("owlFile", outputFile);
            response.put("outputPath", result.outputPath);
            response.put("tableCount", result.tableCount);
            response.put("dataPropertyCount", result.dataPropertyCount);
            response.put("objectPropertyCount", result.objectPropertyCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating OWL ontology", e);
            response.put("success", false);
            response.put("message", "Error generating OWL ontology: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Generate OBDA mapping file from database schema
     */
    @PostMapping(value = "/generate/obda", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateObda(@RequestBody SchemaGenRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Generating OBDA mapping file from database schema...");

            // Create temp directory
            String workDir = System.getProperty("user.dir");
            File tempDir = new File(workDir + "/temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Generate unique filename with timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String outputFile = "mapping_" + timestamp;
            
            ObdaGeneratorArgs args = new ObdaGeneratorArgs();
            args.jdbcUrl = normalizeJdbcUrl(request.jdbcUrl);
            args.user = request.user;
            args.password = request.password;
            args.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            args.catalog = request.catalog;
            args.schema = request.schema;
            args.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            args.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            args.output = "temp/" + outputFile;
            
            if (request.includeTables != null) {
                args.includeTables = Stream.of(request.includeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }
            if (request.excludeTables != null) {
                args.excludeTables = Stream.of(request.excludeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }

            SchemaToObdaGenerator.generate(args);

            response.put("success", true);
            response.put("message", "OBDA mapping file generated successfully");
            response.put("obdaFile", outputFile + ".obda");
            response.put("propertiesFile", outputFile + ".obda.properties");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating OBDA mapping file", e);
            response.put("success", false);
            response.put("message", "Error generating OBDA mapping file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Generate both OWL and OBDA files together
     */
    @PostMapping(value = "/generate/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateAll(@RequestBody SchemaGenRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Generating both OWL ontology and OBDA mapping file...");

            // Create temp directory
            String workDir = System.getProperty("user.dir");
            File tempDir = new File(workDir + "/temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Generate unique filename with timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String owlFileName = "schema_" + timestamp + ".owl";
            String obdaFileName = "mapping_" + timestamp;

            // Generate OWL
            SchemaGeneratorArgs owlArgs = new SchemaGeneratorArgs();
            owlArgs.jdbcUrl = normalizeJdbcUrl(request.jdbcUrl);
            owlArgs.user = request.user;
            owlArgs.password = request.password;
            owlArgs.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            owlArgs.catalog = request.catalog;
            owlArgs.schema = request.schema;
            owlArgs.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            owlArgs.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            owlArgs.output = "temp/" + owlFileName;
            
            if (request.includeTables != null) {
                owlArgs.includeTables = Stream.of(request.includeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }
            if (request.excludeTables != null) {
                owlArgs.excludeTables = Stream.of(request.excludeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }

            SchemaGenerationResult owlResult = SchemaToOwlGenerator.generate(owlArgs);

            // Generate OBDA
            ObdaGeneratorArgs obdaArgs = new ObdaGeneratorArgs();
            obdaArgs.jdbcUrl = normalizeJdbcUrl(request.jdbcUrl);
            obdaArgs.user = request.user;
            obdaArgs.password = request.password;
            obdaArgs.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            obdaArgs.catalog = request.catalog;
            obdaArgs.schema = request.schema;
            obdaArgs.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            obdaArgs.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            obdaArgs.output = "temp/" + obdaFileName;
            
            if (request.includeTables != null) {
                obdaArgs.includeTables = Stream.of(request.includeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }
            if (request.excludeTables != null) {
                obdaArgs.excludeTables = Stream.of(request.excludeTables.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            }

            SchemaToObdaGenerator.generate(obdaArgs);

            response.put("success", true);
            response.put("message", "Both OWL and OBDA files generated successfully");
            response.put("owlFile", owlFileName);
            
            Map<String, Object> owlInfo = new HashMap<>();
            owlInfo.put("tableCount", owlResult.tableCount);
            owlInfo.put("dataPropertyCount", owlResult.dataPropertyCount);
            owlInfo.put("objectPropertyCount", owlResult.objectPropertyCount);
            response.put("owl", owlInfo);
            
            response.put("obdaFile", obdaFileName + ".obda");
            response.put("propertiesFile", obdaFileName + ".obda.properties");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating OWL and OBDA files", e);
            response.put("success", false);
            response.put("message", "Error generating files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== File Download API ==========

    @GetMapping("/files/{fileName}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable String fileName) {
        try {
            String workDir = System.getProperty("user.dir");
            // First try temp directory
            File file = new File(workDir + "/temp", fileName);
            
            if (!file.exists()) {
                // Fallback to root directory
                file = new File(workDir, fileName);
            }
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(file);
            String contentType = fileName.endsWith(".owl") ? "application/xml" :
                                 fileName.endsWith(".obda") ? "text/plain" :
                                 fileName.endsWith(".properties") ? "text/plain" :
                                 "application/octet-stream";
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Request/Response DTOs ==========

    public static class SchemaGenRequest {
        public String jdbcUrl;
        public String user;
        public String password;
        public String driverClass;
        public String catalog;
        public String schema;
        public String tablePattern;
        public String baseIri;
        public String includeTables;
        public String excludeTables;
    }

}
