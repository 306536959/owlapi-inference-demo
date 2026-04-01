package com.example.owlapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Scan relational schema and generate OWL ontology skeleton (classes/properties).
 *
 * Usage:
 *   mvn -q exec:java "-Dexec.mainClass=com.example.owlapi.SchemaToOwlGenerator" "-Dexec.args=--jdbcUrl jdbc:mysql://127.0.0.1:3306/medical_demo --user root --password 123456 --baseIri http://example.com/medical# --output schema-auto.owl"
 */
public class SchemaToOwlGenerator {

  private static final Logger logger = LoggerFactory.getLogger(SchemaToOwlGenerator.class);

  public static void main(String[] args) throws Exception {
    SchemaGeneratorArgs cfg = SchemaGeneratorArgs.fromArgs(args);
    SchemaGenerationResult result = generate(cfg);
    logger.info("Schema scanned and OWL generated.");
    logger.info("Output: {}", result.outputPath);
    logger.info("Tables: {}", result.tableCount);
    logger.info("DataProperties: {}", result.dataPropertyCount);
    logger.info("ObjectProperties(FK): {}", result.objectPropertyCount);
  }

  public static SchemaGenerationResult generate(SchemaGeneratorArgs cfg) throws Exception {
    logger.info("Starting schema scan...");
    logger.info("JDBC URL: {}", cfg.jdbcUrl);
    logger.info("Catalog: {}, Schema: {}, Table pattern: '{}'", cfg.catalog, cfg.schema, cfg.tablePattern);
    logger.info("Include tables: {}, Exclude tables: {}", cfg.includeTables, cfg.excludeTables);

    Class.forName(cfg.driverClass);
    logger.debug("JDBC driver loaded: {}", cfg.driverClass);

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLDataFactory df = manager.getOWLDataFactory();
    OWLOntology ontology = manager.createOntology(IRI.create(cfg.baseIri + "schema"));
    logger.debug("OWL ontology created with IRI: {}", cfg.baseIri + "schema");

    Map<String, OWLClass> tableClassMap = new HashMap<>();
    int dataPropertyCount = 0;
    int objectPropertyCount = 0;

    try (Connection conn = DriverManager.getConnection(cfg.jdbcUrl, cfg.user, cfg.password)) {
      logger.info("Database connection established");
      DatabaseMetaData meta = conn.getMetaData();
      String catalog = cfg.catalog != null ? cfg.catalog : conn.getCatalog();
      String schemaPattern = cfg.schema;
      String tablePattern = cfg.tablePattern == null || cfg.tablePattern.trim().isEmpty() ? "%" : cfg.tablePattern;

      // 1) TABLE -> OWL Class
      logger.info("Scanning tables...");
      int tableCount = 0;
      int skippedTables = 0;
      try (ResultSet tables = meta.getTables(catalog, schemaPattern, tablePattern, new String[] { "TABLE" })) {
        while (tables.next()) {
          String table = tables.getString("TABLE_NAME");
          if (!acceptTable(table, cfg.includeTables, cfg.excludeTables)) {
            logger.debug("Skipping table: {} (filtered by include/exclude rules)", table);
            skippedTables++;
            continue;
          }
          OWLClass cls = df.getOWLClass(IRI.create(cfg.baseIri + normalize(table)));
          tableClassMap.put(table, cls);
          ontology.addAxiom(df.getOWLDeclarationAxiom(cls));
          logger.debug("Mapped table '{}' -> OWL Class: {}", table, cls.getIRI());
          tableCount++;
        }
      }
      logger.info("Scanned {} tables (skipped {}), mapped to OWL Classes", tableCount, skippedTables);

      // 2) COLUMN -> DataProperty + domain/range
      logger.info("Scanning columns for each table...");
      for (Map.Entry<String, OWLClass> entry : tableClassMap.entrySet()) {
        String table = entry.getKey();
        OWLClass tableClass = entry.getValue();
        int columnCount = 0;

        try (ResultSet columns = meta.getColumns(catalog, schemaPattern, table, "%")) {
          while (columns.next()) {
            String column = columns.getString("COLUMN_NAME");
            int sqlType = columns.getInt("DATA_TYPE");

            String dpName = normalize(table + "_" + column);
            OWLDataProperty dp = df.getOWLDataProperty(IRI.create(cfg.baseIri + dpName));
            ontology.addAxiom(df.getOWLDeclarationAxiom(dp));
            ontology.addAxiom(df.getOWLDataPropertyDomainAxiom(dp, tableClass));

            OWLDatatype datatype = mapSqlTypeToXsd(df, sqlType);
            ontology.addAxiom(df.getOWLDataPropertyRangeAxiom(dp, datatype));
            logger.debug("Table '{}': mapped column '{}' -> DataProperty: {} (type: {})", 
                table, column, dp.getIRI(), datatype.getIRI());
            dataPropertyCount++;
            columnCount++;
          }
        }
        logger.debug("Table '{}' has {} columns mapped to DataProperties", table, columnCount);
      }
      logger.info("Total DataProperties created: {}", dataPropertyCount);

      // 3) FK -> ObjectProperty + domain/range
      logger.info("Scanning foreign keys...");
      for (Map.Entry<String, OWLClass> entry : tableClassMap.entrySet()) {
        String fkTable = entry.getKey();
        OWLClass fkClass = entry.getValue();

        try (ResultSet fks = meta.getImportedKeys(catalog, schemaPattern, fkTable)) {
          while (fks.next()) {
            String pkTable = fks.getString("PKTABLE_NAME");
            String fkName = fks.getString("FK_NAME");
            String fkColumn = fks.getString("FKCOLUMN_NAME");
            String pkColumn = fks.getString("PKCOLUMN_NAME");
            OWLClass pkClass = tableClassMap.get(pkTable);
            if (pkClass == null) {
              logger.warn("FK references unknown table '{}', skipping", pkTable);
              continue;
            }

            String opName = normalize("ref_" + fkTable + "_to_" + pkTable + "_" + (fkName == null ? "fk" : fkName));
            OWLObjectProperty op = df.getOWLObjectProperty(IRI.create(cfg.baseIri + opName));
            ontology.addAxiom(df.getOWLDeclarationAxiom(op));
            ontology.addAxiom(df.getOWLObjectPropertyDomainAxiom(op, fkClass));
            ontology.addAxiom(df.getOWLObjectPropertyRangeAxiom(op, pkClass));
            logger.debug("FK '{}.{}' -> '{}.{}': mapped to ObjectProperty: {}", 
                fkTable, fkColumn, pkTable, pkColumn, op.getIRI());
            objectPropertyCount++;
          }
        }
      }
      logger.info("Total ObjectProperties (FK) created: {}", objectPropertyCount);
    }

    File out = new File(cfg.output);
    File parent = out.getParentFile();
    if (parent != null) {
      parent.mkdirs();
      logger.debug("Created output directory: {}", parent.getAbsolutePath());
    }

    manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(out));
    logger.info("OWL ontology saved to: {}", out.getAbsolutePath());
    logger.info("Total axioms in ontology: {}", ontology.getAxiomCount());

    SchemaGenerationResult result = new SchemaGenerationResult();
    result.tableCount = tableClassMap.size();
    result.dataPropertyCount = dataPropertyCount;
    result.objectPropertyCount = objectPropertyCount;
    result.outputPath = out.getAbsolutePath();
    return result;
  }

  private static OWLDatatype mapSqlTypeToXsd(OWLDataFactory df, int sqlType) {
    switch (sqlType) {
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.TINYINT:
        return df.getIntegerOWLDatatype();
      case Types.BIGINT:
        return df.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#long"));
      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
        return df.getDoubleOWLDatatype();
      case Types.DECIMAL:
      case Types.NUMERIC:
        return df.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#decimal"));
      case Types.BOOLEAN:
      case Types.BIT:
        return df.getBooleanOWLDatatype();
      case Types.DATE:
        return df.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#date"));
      case Types.TIME:
        return df.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#time"));
      case Types.TIMESTAMP:
      case Types.TIMESTAMP_WITH_TIMEZONE:
        return df.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#dateTime"));
      default:
        return df.getStringOWLDatatype();
    }
  }

  private static String normalize(String raw) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    return sb.toString();
  }

  private static boolean acceptTable(String table, Set<String> include, Set<String> exclude) {
    String t = table == null ? "" : table.toLowerCase();
    if (exclude != null && exclude.contains(t)) return false;
    if (include == null || include.isEmpty()) return true;
    return include.contains(t);
  }
}

