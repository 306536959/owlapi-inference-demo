package com.example.owlapi.bootstrap;

import com.example.owlapi.SchemaGenerationResult;
import com.example.owlapi.SchemaGeneratorArgs;
import com.example.owlapi.SchemaToOwlGenerator;
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

    SchemaGenerationResult result = SchemaToOwlGenerator.generate(sg);
    logger.info("Schema bootstrap completed successfully!");
    logger.info("  Tables mapped: {}", result.tableCount);
    logger.info("  DataProperties created: {}", result.dataPropertyCount);
    logger.info("  ObjectProperties (FK) created: {}", result.objectPropertyCount);
    logger.info("  Output: {}", result.outputPath);
  }
}

