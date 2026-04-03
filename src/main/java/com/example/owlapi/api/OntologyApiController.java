package com.example.owlapi.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OntologyApiController {

  private final SparqlGatewayService sparqlGatewayService;

  public OntologyApiController(SparqlGatewayService sparqlGatewayService) {
    this.sparqlGatewayService = sparqlGatewayService;
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
}

