package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class OntologyApiController {

  private final SparqlGatewayService sparqlGatewayService;
  private final SystemBuiltinProperties props;
  private final GraphDbImportService graphDbImportService;

  public OntologyApiController(SparqlGatewayService sparqlGatewayService, SystemBuiltinProperties props, GraphDbImportService graphDbImportService) {
    this.sparqlGatewayService = sparqlGatewayService;
    this.props = props;
    this.graphDbImportService = graphDbImportService;
  }

  @GetMapping("/health")
  public String health() {
    return "ok, using Ontop SPARQL service";
  }

  @PostMapping(value = "/sparql/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public SparqlQueryResponse query(@RequestBody SparqlQueryRequest request) {
    if (request == null || request.query == null || request.query.trim().isEmpty()) {
      throw new IllegalArgumentException("query is required");
    }
    String result = sparqlGatewayService.executeSelectJson(request.query);
    SparqlQueryResponse response = new SparqlQueryResponse();
    response.endpoint = "Ontop SPARQL Service";
    response.resultJson = result;
    return response;
  }

  @PostMapping(value = "/mapping/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> uploadMappingFile(@RequestParam("file") MultipartFile file, @RequestParam("repositoryId") String repositoryId) {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("File is empty");
    }

    if (!file.getOriginalFilename().endsWith(".obda")) {
      return ResponseEntity.badRequest().body("File must be an .obda file");
    }

    try {
      byte[] fileContent = file.getBytes();
      graphDbImportService.importMappingFile(repositoryId, fileContent);
      return ResponseEntity.ok("Mapping file uploaded successfully");
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading mapping file: " + e.getMessage());
    }
  }
}

