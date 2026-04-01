package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SparqlGatewayService {

  private static final Logger logger = LoggerFactory.getLogger(SparqlGatewayService.class);
  private final SystemBuiltinProperties props;
  private final GraphDbService graphDbService;

  public SparqlGatewayService(SystemBuiltinProperties props, GraphDbService graphDbService) {
    this.props = props;
    this.graphDbService = graphDbService;
  }

  public String executeSelectJson(String query) {
    try {
      logger.debug("Executing SPARQL query via GraphDB: {}", query);
      return graphDbService.executeSparqlQuery(query);
    } catch (IOException e) {
      logger.error("Error executing SPARQL query via GraphDB", e);
      throw new RuntimeException("Failed to execute SPARQL query: " + e.getMessage(), e);
    }
  }
}

