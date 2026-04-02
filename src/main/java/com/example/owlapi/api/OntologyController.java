package com.example.owlapi.api;

import com.example.owlapi.SchemaGenerationResult;
import com.example.owlapi.SchemaGeneratorArgs;
import com.example.owlapi.SchemaToOwlGenerator;
import com.example.owlapi.ObdaGeneratorArgs;
import com.example.owlapi.SchemaToObdaGenerator;
import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbImportService;
import com.example.owlapi.graphdb.GraphDbService;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/ontology")
public class OntologyController {

    private static final Logger logger = LoggerFactory.getLogger(OntologyController.class);
    private final SystemBuiltinProperties props;
    private final GraphDbImportService graphDbImportService;
    private final GraphDbService graphDbService;

    public OntologyController(SystemBuiltinProperties props, 
                                GraphDbImportService graphDbImportService,
                                GraphDbService graphDbService) {
        this.props = props;
        this.graphDbImportService = graphDbImportService;
        this.graphDbService = graphDbService;
    }

    // ========== Schema Generation APIs ==========

    /**
     * Generate OWL ontology from database schema
     */
    @PostMapping(value = "/generate/owl", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateOwl(@RequestBody SchemaGenRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("Generating OWL ontology from database schema...");

            SchemaGeneratorArgs args = new SchemaGeneratorArgs();
            args.jdbcUrl = request.jdbcUrl;
            args.user = request.user;
            args.password = request.password;
            args.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            args.catalog = request.catalog;
            args.schema = request.schema;
            args.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            args.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            args.output = request.outputFile != null ? request.outputFile : "schema-auto.owl";
            
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

            ObdaGeneratorArgs args = new ObdaGeneratorArgs();
            args.jdbcUrl = request.jdbcUrl;
            args.user = request.user;
            args.password = request.password;
            args.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            args.catalog = request.catalog;
            args.schema = request.schema;
            args.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            args.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            args.output = request.obdaOutputFile != null ? request.obdaOutputFile : "schema-auto.obda";
            
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
            response.put("obdaFile", args.output + ".obda");
            response.put("propertiesFile", args.output + ".properties");
            
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

            // Generate OWL
            SchemaGeneratorArgs owlArgs = new SchemaGeneratorArgs();
            owlArgs.jdbcUrl = request.jdbcUrl;
            owlArgs.user = request.user;
            owlArgs.password = request.password;
            owlArgs.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            owlArgs.catalog = request.catalog;
            owlArgs.schema = request.schema;
            owlArgs.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            owlArgs.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            owlArgs.output = request.outputFile != null ? request.outputFile : "schema-auto.owl";
            
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
            obdaArgs.jdbcUrl = request.jdbcUrl;
            obdaArgs.user = request.user;
            obdaArgs.password = request.password;
            obdaArgs.driverClass = request.driverClass != null ? request.driverClass : "com.mysql.cj.jdbc.Driver";
            obdaArgs.catalog = request.catalog;
            obdaArgs.schema = request.schema;
            obdaArgs.tablePattern = request.tablePattern != null ? request.tablePattern : "%";
            obdaArgs.baseIri = request.baseIri != null ? request.baseIri : "http://example.com/ontology#";
            obdaArgs.output = request.obdaOutputFile != null ? request.obdaOutputFile : "schema-auto.obda";
            
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
            response.put("owlFile", owlResult.outputPath);
            
            Map<String, Object> owlInfo = new HashMap<>();
            owlInfo.put("tableCount", owlResult.tableCount);
            owlInfo.put("dataPropertyCount", owlResult.dataPropertyCount);
            owlInfo.put("objectPropertyCount", owlResult.objectPropertyCount);
            response.put("owl", owlInfo);
            
            response.put("obdaFile", obdaArgs.output + ".obda");
            response.put("propertiesFile", obdaArgs.output + ".properties");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating OWL and OBDA files", e);
            response.put("success", false);
            response.put("message", "Error generating files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== File Upload APIs ==========

    /**
     * Upload OWL ontology file
     */
    @PostMapping(value = "/upload/owl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadOwl(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "repositoryId", required = false) String repositoryId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.endsWith(".owl") && !originalFilename.endsWith(".rdf") && !originalFilename.endsWith(".ttl"))) {
                response.put("success", false);
                response.put("message", "File must be .owl, .rdf, or .ttl format");
                return ResponseEntity.badRequest().body(response);
            }

            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String savedPath = uploadDir + "ontology." + (originalFilename.endsWith(".ttl") ? "ttl" : "owl");
            file.transferTo(new File(savedPath));

            response.put("success", true);
            response.put("message", "OWL file uploaded successfully");
            response.put("filePath", savedPath);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());

            // Optionally import to repository
            if (repositoryId != null && !repositoryId.isEmpty()) {
                try {
                    graphDbImportService.importOwlFileToRepository(repositoryId, new FileInputStream(new File(savedPath)), 
                        originalFilename.endsWith(".ttl") ? RDFFormat.TURTLE : RDFFormat.RDFXML);
                    response.put("imported", true);
                    response.put("repositoryId", repositoryId);
                } catch (Exception e) {
                    logger.warn("Failed to import OWL to repository: {}", e.getMessage());
                    response.put("imported", false);
                    response.put("importError", e.getMessage());
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading OWL file", e);
            response.put("success", false);
            response.put("message", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload OBDA mapping file
     */
    @PostMapping(value = "/upload/obda", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadObda(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".obda")) {
                response.put("success", false);
                response.put("message", "File must be .obda format");
                return ResponseEntity.badRequest().body(response);
            }

            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String savedPath = uploadDir + "mapping.obda";
            file.transferTo(new File(savedPath));

            response.put("success", true);
            response.put("message", "OBDA file uploaded successfully");
            response.put("filePath", savedPath);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading OBDA file", e);
            response.put("success", false);
            response.put("message", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload RDF/TTL data file for import
     */
    @PostMapping(value = "/upload/rdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> uploadRdf(@RequestParam("file") MultipartFile file,
                                                           @RequestParam("repositoryId") String repositoryId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            RDFFormat format = RDFFormat.RDFXML;
            if (originalFilename != null) {
                if (originalFilename.endsWith(".ttl")) {
                    format = RDFFormat.TURTLE;
                } else if (originalFilename.endsWith(".nt")) {
                    format = RDFFormat.NTRIPLES;
                } else if (originalFilename.endsWith(".n3")) {
                    format = RDFFormat.N3;
                }
            }

            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String savedPath = uploadDir + "data." + (format == RDFFormat.TURTLE ? "ttl" : format == RDFFormat.NTRIPLES ? "nt" : "rdf");
            file.transferTo(new File(savedPath));

            // Import to repository
            graphDbImportService.importRdfToRepository(repositoryId, new FileInputStream(new File(savedPath)), format);

            response.put("success", true);
            response.put("message", "RDF file uploaded and imported successfully");
            response.put("filePath", savedPath);
            response.put("fileName", file.getOriginalFilename());
            response.put("repositoryId", repositoryId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading RDF file", e);
            response.put("success", false);
            response.put("message", "Error uploading/importing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== File Download/View APIs ==========

    /**
     * Get generated files list
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> getGeneratedFiles() {
        Map<String, Object> response = new HashMap<>();
        try {
            String workDir = System.getProperty("user.dir");
            File uploadsDir = new File(workDir + "/uploads");
            File rootDir = new File(workDir);
            
            Map<String, Object> files = new HashMap<>();
            
            // Check generated files
            File owlFile = new File(rootDir, "schema-auto.owl");
            if (owlFile.exists()) {
                Map<String, Object> owlInfo = new HashMap<>();
                owlInfo.put("name", owlFile.getName());
                owlInfo.put("path", owlFile.getAbsolutePath());
                owlInfo.put("size", owlFile.length());
                owlInfo.put("lastModified", owlFile.lastModified());
                files.put("owl", owlInfo);
            }
            
            File obdaFile = new File(rootDir, "schema-auto.obda.obda");
            if (!obdaFile.exists()) {
                obdaFile = new File(rootDir, "schema-auto.obda");
            }
            if (obdaFile.exists()) {
                Map<String, Object> obdaInfo = new HashMap<>();
                obdaInfo.put("name", obdaFile.getName());
                obdaInfo.put("path", obdaFile.getAbsolutePath());
                obdaInfo.put("size", obdaFile.length());
                obdaInfo.put("lastModified", obdaFile.lastModified());
                files.put("obda", obdaInfo);
            }
            
            File propsFile = new File(rootDir, "schema-auto.obda.properties");
            if (propsFile.exists()) {
                Map<String, Object> propsInfo = new HashMap<>();
                propsInfo.put("name", propsFile.getName());
                propsInfo.put("path", propsFile.getAbsolutePath());
                propsInfo.put("size", propsFile.length());
                propsInfo.put("lastModified", propsFile.lastModified());
                files.put("properties", propsInfo);
            }
            
            // List upload files
            if (uploadsDir.exists()) {
                File[] uploadFiles = uploadsDir.listFiles();
                if (uploadFiles != null) {
                    List<Map<String, Object>> uploadList = new java.util.ArrayList<>();
                    for (File f : uploadFiles) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", f.getName());
                        fileInfo.put("path", f.getAbsolutePath());
                        fileInfo.put("size", f.length());
                        fileInfo.put("lastModified", f.lastModified());
                        uploadList.add(fileInfo);
                    }
                    files.put("uploads", uploadList);
                }
            }
            
            response.put("success", true);
            response.put("files", files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting file list", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Read file content
     */
    @GetMapping("/files/{fileName}/content")
    public ResponseEntity<Map<String, Object>> getFileContent(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        try {
            String workDir = System.getProperty("user.dir");
            File file = new File(workDir, fileName);
            
            if (!file.exists()) {
                // Try uploads directory
                file = new File(workDir + "/uploads", fileName);
            }
            
            if (!file.exists()) {
                response.put("success", false);
                response.put("message", "File not found: " + fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            String content = new String(Files.readAllBytes(file.toPath()));
            
            response.put("success", true);
            response.put("fileName", file.getName());
            response.put("content", content);
            response.put("size", file.length());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error reading file content", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
        public String outputFile;
        public String obdaOutputFile;
    }
}
