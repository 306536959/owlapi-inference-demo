package com.example.owlapi;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaToOwlGeneratorTest {

  @Test
  void shouldGenerateOwlFileFromRelationalSchema() throws Exception {
    String jdbcUrl = "jdbc:h2:mem:owlgen;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
         Statement st = conn.createStatement()) {
      st.execute("CREATE TABLE doctor (id INT PRIMARY KEY, doctor_name VARCHAR(128))");
      st.execute("CREATE TABLE patient (id INT PRIMARY KEY, patient_code VARCHAR(64))");
      st.execute("CREATE TABLE medical_record (id INT PRIMARY KEY, notes VARCHAR(255), doctor_id INT, patient_id INT)");
      st.execute("ALTER TABLE medical_record ADD CONSTRAINT fk_mr_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id)");
      st.execute("ALTER TABLE medical_record ADD CONSTRAINT fk_mr_patient FOREIGN KEY (patient_id) REFERENCES patient(id)");
    }

    Path out = Files.createTempFile("schema-auto-", ".owl");
    Files.deleteIfExists(out);

    SchemaGeneratorArgs args = new SchemaGeneratorArgs();
    args.jdbcUrl = jdbcUrl;
    args.user = "sa";
    args.password = "";
    args.baseIri = "http://example.com/medical#";
    args.output = out.toAbsolutePath().toString();
    args.driverClass = "org.h2.Driver";

    SchemaGenerationResult result = SchemaToOwlGenerator.generate(args);

    assertTrue(Files.exists(out), "OWL file should be generated");
    assertTrue(result.tableCount >= 3, "Should discover at least 3 tables");
    assertTrue(result.dataPropertyCount >= 4, "Should discover data properties from columns");
    assertTrue(result.objectPropertyCount >= 2, "Should discover object properties from foreign keys");

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(out.toFile());

    OWLClass doctorClass = manager.getOWLDataFactory().getOWLClass(IRI.create("http://example.com/medical#doctor"));
    OWLDataProperty doctorNameProperty =
        manager.getOWLDataFactory().getOWLDataProperty(IRI.create("http://example.com/medical#doctor_doctor_name"));
    OWLObjectProperty fkProperty =
        manager.getOWLDataFactory().getOWLObjectProperty(IRI.create("http://example.com/medical#ref_medical_record_to_doctor_fk_mr_doctor"));

    assertTrue(ontology.containsClassInSignature(doctorClass.getIRI()), "doctor class must exist");
    assertTrue(ontology.containsDataPropertyInSignature(doctorNameProperty.getIRI()), "doctor_name data property must exist");
    assertTrue(ontology.containsObjectPropertyInSignature(fkProperty.getIRI()), "foreign key object property must exist");
  }
}

