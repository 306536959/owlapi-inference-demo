package com.example.owlapi.bootstrap;

import com.example.owlapi.SchemaGenerationResult;
import com.example.owlapi.SchemaGeneratorArgs;
import com.example.owlapi.SchemaToOwlGenerator;
import com.example.owlapi.SchemaToObdaGenerator;
import com.example.owlapi.ObdaGeneratorArgs;
import com.example.owlapi.config.SystemBuiltinProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class SchemaBootstrapRunner implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(SchemaBootstrapRunner.class);

  private final SystemBuiltinProperties props;

  public SchemaBootstrapRunner(SystemBuiltinProperties props) {
    this.props = props;
  }

  @Override
  public void run(String... args) throws Exception {
    SystemBuiltinProperties.Bootstrap b = props.getBootstrap();
    if (!b.isEnabled()) {
      logger.info("Schema bootstrap is disabled (ontology.bootstrap.enabled=false)");
      return;
    }
    logger.info("Schema bootstrap is enabled, starting...");

    if (b.getJdbcUrl() == null || b.getJdbcUrl().trim().isEmpty()) {
      logger.error("ontology.bootstrap.enabled=true but ontology.bootstrap.jdbc-url is empty");
      throw new IllegalArgumentException("ontology.bootstrap.enabled=true but ontology.bootstrap.jdbc-url is empty");
    }

    // 1) Generate OWL ontology
    logger.info("=== Step 1: Generating OWL ontology ===");
    SchemaGeneratorArgs sg = new SchemaGeneratorArgs();
    sg.jdbcUrl = b.getJdbcUrl();
    sg.user = b.getUser();
    sg.password = b.getPassword();
    sg.catalog = b.getCatalog();
    sg.schema = b.getSchema();
    sg.tablePattern = b.getTablePattern();
    sg.baseIri = b.getBaseIri();
    sg.output = b.getOutput();
    sg.driverClass = b.getDriverClass();
    sg.includeTables = new HashSet<>();
    for (String t : b.getIncludeTables()) sg.includeTables.add(t.toLowerCase());
    sg.excludeTables = new HashSet<>();
    for (String t : b.getExcludeTables()) sg.excludeTables.add(t.toLowerCase());

    logger.info("Schema bootstrap configuration:");
    logger.info("  JDBC URL: {}", sg.jdbcUrl);
    logger.info("  Catalog: {}, Schema: {}", sg.catalog, sg.schema);
    logger.info("  Output file: {}", sg.output);
    logger.info("  Base IRI: {}", sg.baseIri);

    SchemaGenerationResult owlResult = SchemaToOwlGenerator.generate(sg);
    logger.info("OWL ontology generation completed!");
    logger.info("  Tables mapped: {}", owlResult.tableCount);
    logger.info("  DataProperties created: {}", owlResult.dataPropertyCount);
    logger.info("  ObjectProperties (FK) created: {}", owlResult.objectPropertyCount);
    logger.info("  Output: {}", owlResult.outputPath);

    // 2) Generate OBDA mapping
    logger.info("");
    logger.info("=== Step 2: Generating OBDA mapping files ===");
    ObdaGeneratorArgs og = new ObdaGeneratorArgs();
    og.jdbcUrl = b.getJdbcUrl();
    og.user = b.getUser();
    og.password = b.getPassword();
    og.catalog = b.getCatalog();
    og.schema = b.getSchema();
    og.tablePattern = b.getTablePattern();
    og.baseIri = b.getBaseIri();
    og.output = b.getObdaOutput();
    og.driverClass = b.getDriverClass();
    og.includeTables = new HashSet<>();
    for (String t : b.getIncludeTables()) og.includeTables.add(t.toLowerCase());
    og.excludeTables = new HashSet<>();
    for (String t : b.getExcludeTables()) og.excludeTables.add(t.toLowerCase());

    logger.info("OBDA generation configuration:");
    logger.info("  JDBC URL: {}", og.jdbcUrl);
    logger.info("  Output base: {}", og.output);

    SchemaToObdaGenerator.generate(og);
    logger.info("OBDA mapping generation completed!");

    logger.info("");
    logger.info("=== Bootstrap Summary ===");
    logger.info("Generated files:");
    logger.info("  - OWL ontology: {}", owlResult.outputPath);
    logger.info("  - OBDA mappings: {}.obda", og.output);
    logger.info("  - OBDA config: {}.properties", og.output);
    logger.info("");
    logger.info("To import OBDA into GraphDB:");
    logger.info("  1. Open GraphDB Workbench (http://192.168.10.97:7200)");
    logger.info("  2. Go to Import -> OBDA models");
    logger.info("  3. Upload {}.obda and {}.properties", og.output, og.output);
  }
}

