package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * OWL/OBDA 本体编辑器 API
 *
 * 功能：
 * 1. 读取并解析仓库的 OWL 文件，提取类、属性结构
 * 2. 通过 OWLAPI 操作本体（增删类、属性、关系）
 * 3. 追加 OBDA 映射规则
 * 4. 上传新 OWL/OBDA 文件覆盖现有文件
 * 5. 保存文件并触发 GraphDB 仓库热重启
 */
@RestController
@RequestMapping("/api/ontology/editor")
public class OntologyEditorController {

    private static final Logger logger = LoggerFactory.getLogger(OntologyEditorController.class);
    private final SystemBuiltinProperties props;

    public OntologyEditorController(SystemBuiltinProperties props) {
        this.props = props;
    }

    // ========== 辅助方法 ==========

    private String readBaseIri(String propertiesPath) {
        try {
            String content = new String(Files.readAllBytes(new File(propertiesPath).toPath()), StandardCharsets.UTF_8);
            for (String line : content.split("\n")) {
                if (line.startsWith("base.iri=")) {
                    return line.substring("base.iri=".length()).trim();
                }
            }
        } catch (Exception ignored) {}
        return "http://example.com/ontology#";
    }

    private String extractPrefix(String iri, String baseIri) {
        if (baseIri != null && iri.startsWith(baseIri)) {
            return iri.substring(baseIri.length());
        }
        return iri;
    }

    private int countMappings(File obdaFile) {
        try {
            String content = new String(Files.readAllBytes(obdaFile.toPath()), StandardCharsets.UTF_8);
            int count = 0, idx = 0;
            while ((idx = content.indexOf("mappingId", idx)) != -1) { count++; idx++; }
            return count;
        } catch (Exception e) { return 0; }
    }

    // 收集某类的 SubClassOf axiom (过滤 owl:Thing)
    private Set<OWLSubClassOfAxiom> getSubClassAxioms(OWLOntology ontology, OWLClass cls) {
        Set<OWLSubClassOfAxiom> result = new HashSet<>();
        for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            if (ax.getSubClass().equals(cls) && !ax.getSuperClass().isOWLThing()) {
                result.add(ax);
            }
        }
        return result;
    }

    // 收集某类的 EquivalentClasses axiom
    private Set<OWLEquivalentClassesAxiom> getEquivClassAxioms(OWLOntology ontology, OWLClass cls) {
        Set<OWLEquivalentClassesAxiom> result = new HashSet<>();
        for (OWLEquivalentClassesAxiom ax : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            if (ax.getClassesInSignature().contains(cls)) {
                result.add(ax);
            }
        }
        return result;
    }

    // 收集某属性的 ObjectPropertyDomain axiom
    private Set<OWLObjectPropertyDomainAxiom> getOPDomainAxioms(OWLOntology ontology, OWLObjectProperty op) {
        Set<OWLObjectPropertyDomainAxiom> result = new HashSet<>();
        for (OWLObjectPropertyDomainAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
            if (ax.getProperty().equals(op)) {
                result.add(ax);
            }
        }
        return result;
    }

    // 收集某属性的 ObjectPropertyRange axiom
    private Set<OWLObjectPropertyRangeAxiom> getOPRangeAxioms(OWLOntology ontology, OWLObjectProperty op) {
        Set<OWLObjectPropertyRangeAxiom> result = new HashSet<>();
        for (OWLObjectPropertyRangeAxiom ax : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE)) {
            if (ax.getProperty().equals(op)) {
                result.add(ax);
            }
        }
        return result;
    }

    // 收集某属性的 SubObjectProperty axiom
    private Set<OWLSubObjectPropertyOfAxiom> getSubOPAxioms(OWLOntology ontology, OWLObjectProperty op) {
        Set<OWLSubObjectPropertyOfAxiom> result = new HashSet<>();
        for (OWLSubObjectPropertyOfAxiom ax : ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY)) {
            if (ax.getSubProperty().equals(op)) {
                result.add(ax);
            }
        }
        return result;
    }

    // 收集某属性的 InverseObjectProperty axiom
    private Set<OWLInverseObjectPropertiesAxiom> getInvOPAxioms(OWLOntology ontology, OWLObjectProperty op) {
        Set<OWLInverseObjectPropertiesAxiom> result = new HashSet<>();
        for (OWLInverseObjectPropertiesAxiom ax : ontology.getAxioms(AxiomType.INVERSE_OBJECT_PROPERTIES)) {
            if (ax.getFirstProperty().equals(op) || ax.getSecondProperty().equals(op)) {
                result.add(ax);
            }
        }
        return result;
    }

    // 通过 AxiomType 检查某 ObjectProperty 是否有特定 axiom
    private boolean hasOPChar(OWLOntology ontology, OWLObjectProperty op, AxiomType<?> type) {
        if (type == AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY) {
            for (OWLInverseFunctionalObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (type == AxiomType.ASYMMETRIC_OBJECT_PROPERTY) {
            for (OWLAsymmetricObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.ASYMMETRIC_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (type == AxiomType.REFLEXIVE_OBJECT_PROPERTY) {
            for (OWLReflexiveObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.REFLEXIVE_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (type == AxiomType.IRREFLEXIVE_OBJECT_PROPERTY) {
            for (OWLIrreflexiveObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        }
        return false;
    }

    private boolean hasOPAxiom(OWLOntology ontology, OWLObjectProperty op, Class<? extends OWLAxiom> cls) {
        if (cls == OWLFunctionalObjectPropertyAxiom.class) {
            for (OWLFunctionalObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.FUNCTIONAL_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (cls == OWLTransitiveObjectPropertyAxiom.class) {
            for (OWLTransitiveObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.TRANSITIVE_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (cls == OWLSymmetricObjectPropertyAxiom.class) {
            for (OWLSymmetricObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.SYMMETRIC_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        } else if (cls == OWLFunctionalDataPropertyAxiom.class) {
            for (OWLFunctionalDataPropertyAxiom ax : ontology.getAxioms(AxiomType.FUNCTIONAL_DATA_PROPERTY)) {
                if (op.equals(ax.getProperty())) return true;
            }
        }
        return false;
    }

    // ========== 仓库文件信息 ==========

    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> getEditableFiles(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String basePath = System.getProperty("user.dir") + "/uploads/" + repoId + "/";
            File owlFile = new File(basePath + "ontology.owl");
            File obdaFile = new File(basePath + "mapping.obda");
            Map<String, Object> files = new HashMap<>();

            if (owlFile.exists()) {
                Map<String, Object> owlInfo = new LinkedHashMap<>();
                owlInfo.put("name", owlFile.getName());
                owlInfo.put("size", owlFile.length());
                owlInfo.put("lastModified", owlFile.lastModified());
                try {
                    OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
                    OWLOntology ont = mgr.loadOntologyFromOntologyDocument(owlFile);
                    owlInfo.put("ontologyIRI", ont.getOntologyID().getOntologyIRI().map(IRI::toString).orElse(""));
                    owlInfo.put("classCount", ont.getClassesInSignature().size());
                    owlInfo.put("objectPropertyCount", ont.getObjectPropertiesInSignature().size());
                    owlInfo.put("dataPropertyCount", ont.getDataPropertiesInSignature().size());
                    owlInfo.put("axiomCount", ont.getAxiomCount());
                } catch (Exception e) { /* ignore */ }
                files.put("owl", owlInfo);
            }

            if (obdaFile.exists()) {
                Map<String, Object> obdaInfo = new HashMap<>();
                obdaInfo.put("name", obdaFile.getName());
                obdaInfo.put("size", obdaFile.length());
                obdaInfo.put("lastModified", obdaFile.lastModified());
                obdaInfo.put("mappingCount", countMappings(obdaFile));
                files.put("obda", obdaInfo);
            }

            response.put("success", true);
            response.put("repoId", repoId);
            response.put("files", files);
            response.put("baseIri", readBaseIri(basePath + "config.properties"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting editable files for repo: {}", repoId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== OWL 本体读取 ==========

    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> getClasses(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = readBaseIri(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");
            List<Map<String, Object>> classes = new ArrayList<>();

            for (OWLClass cls : ontology.getClassesInSignature()) {
                String iriStr = cls.getIRI().toString();
                if (iriStr.startsWith("http://www.w3.org/")) continue;

                Map<String, Object> info = new HashMap<>();
                info.put("iri", iriStr);
                info.put("localName", cls.getIRI().getShortForm());
                info.put("prefix", extractPrefix(iriStr, baseIri));

                // 父类
                List<String> superClasses = new ArrayList<>();
                for (OWLSubClassOfAxiom ax : getSubClassAxioms(ontology, cls)) {
                    superClasses.add(ax.getSuperClass().asOWLClass().getIRI().toString());
                }
                info.put("superClasses", superClasses);

                // 等价类
                List<String> equivClasses = new ArrayList<>();
                for (OWLEquivalentClassesAxiom ax : getEquivClassAxioms(ontology, cls)) {
                    for (OWLClass ec : ax.getClassesInSignature()) {
                        if (!ec.equals(cls) && !ec.isOWLThing()) {
                            equivClasses.add(ec.getIRI().toString());
                        }
                    }
                }
                info.put("equivalentClasses", equivClasses);

                // 描述
                String comment = "";
                String label = cls.getIRI().getShortForm();
                for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
                    OWLAnnotationProperty prop = ax.getProperty();
                    IRI propIri = prop.getIRI();
                    if (propIri.getRemainder().isPresent() && propIri.getRemainder().get().equals("comment")) {
                        comment = ax.getValue().toString();
                    }
                    if (propIri.getRemainder().isPresent() && propIri.getRemainder().get().equals("label")) {
                        OWLLiteral lit = ax.getValue().asLiteral().orElse(null);
                        if (lit != null) label = lit.getLiteral();
                    }
                }
                info.put("comment", comment);
                info.put("label", label);
                classes.add(info);
            }

            response.put("success", true);
            response.put("baseIri", baseIri);
            response.put("classes", classes);
            response.put("total", classes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting classes", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/objectproperties")
    public ResponseEntity<Map<String, Object>> getObjectProperties(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = readBaseIri(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");
            List<Map<String, Object>> props = new ArrayList<>();

            for (OWLObjectProperty op : ontology.getObjectPropertiesInSignature()) {
                String iriStr = op.getIRI().toString();
                if (iriStr.startsWith("http://www.w3.org/")) continue;

                Map<String, Object> info = new HashMap<>();
                info.put("iri", iriStr);
                info.put("localName", op.getIRI().getShortForm());
                info.put("prefix", extractPrefix(iriStr, baseIri));

                // Domain
                List<String> domains = new ArrayList<>();
                for (OWLObjectPropertyDomainAxiom ax : getOPDomainAxioms(ontology, op)) {
                    domains.add(ax.getDomain().asOWLClass().getIRI().toString());
                }
                info.put("domains", domains);

                // Range
                List<String> ranges = new ArrayList<>();
                for (OWLObjectPropertyRangeAxiom ax : getOPRangeAxioms(ontology, op)) {
                    ranges.add(ax.getRange().asOWLClass().getIRI().toString());
                }
                info.put("ranges", ranges);

                // Super properties
                List<String> superProps = new ArrayList<>();
                for (OWLSubObjectPropertyOfAxiom ax : getSubOPAxioms(ontology, op)) {
                    superProps.add(ax.getSuperProperty().asOWLObjectProperty().getIRI().toString());
                }
                info.put("superProperties", superProps);

                // Characteristics
                Map<String, Boolean> chars = new HashMap<>();
                chars.put("functional", !ontology.getFunctionalObjectPropertyAxioms(op).isEmpty());
                chars.put("inverseFunctional", hasOPChar(ontology, op, AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY));
                chars.put("transitive", !ontology.getTransitiveObjectPropertyAxioms(op).isEmpty());
                chars.put("symmetric", !ontology.getSymmetricObjectPropertyAxioms(op).isEmpty());
                chars.put("asymmetric", hasOPChar(ontology, op, AxiomType.ASYMMETRIC_OBJECT_PROPERTY));
                chars.put("reflexive", hasOPChar(ontology, op, AxiomType.REFLEXIVE_OBJECT_PROPERTY));
                chars.put("irreflexive", hasOPChar(ontology, op, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY));
                info.put("characteristics", chars);

                // Inverse property
                String inverseProp = null;
                for (OWLInverseObjectPropertiesAxiom ax : getInvOPAxioms(ontology, op)) {
                    OWLObjectProperty inv = ax.getFirstProperty().equals(op)
                            ? ax.getSecondProperty().asOWLObjectProperty()
                            : ax.getFirstProperty().asOWLObjectProperty();
                    inverseProp = inv.getIRI().toString();
                    break;
                }
                info.put("inverseProperty", inverseProp);

                props.add(info);
            }

            response.put("success", true);
            response.put("baseIri", baseIri);
            response.put("properties", props);
            response.put("total", props.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting object properties", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/dataproperties")
    public ResponseEntity<Map<String, Object>> getDataProperties(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = readBaseIri(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");
            List<Map<String, Object>> props = new ArrayList<>();

            for (OWLDataProperty dp : ontology.getDataPropertiesInSignature()) {
                String iriStr = dp.getIRI().toString();
                if (iriStr.startsWith("http://www.w3.org/")) continue;

                Map<String, Object> info = new HashMap<>();
                info.put("iri", iriStr);
                info.put("localName", dp.getIRI().getShortForm());
                info.put("prefix", extractPrefix(iriStr, baseIri));

                // Domain
                List<String> domains = new ArrayList<>();
                for (OWLDataPropertyDomainAxiom ax : ontology.getDataPropertyDomainAxioms(dp)) {
                    domains.add(ax.getDomain().asOWLClass().getIRI().toString());
                }
                info.put("domains", domains);

                // Range
                List<String> rangeList = new ArrayList<>();
                for (OWLDataPropertyRangeAxiom ax : ontology.getDataPropertyRangeAxioms(dp)) {
                    rangeList.add(ax.getRange().asOWLDatatype().getIRI().toString());
                }
                info.put("ranges", rangeList);

                // Functional
                Map<String, Boolean> chars = new HashMap<>();
                chars.put("functional", !ontology.getFunctionalDataPropertyAxioms(dp).isEmpty());
                info.put("characteristics", chars);

                props.add(info);
            }

            response.put("success", true);
            response.put("baseIri", baseIri);
            response.put("properties", props);
            response.put("total", props.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting data properties", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== OWL 类写入 ==========

    @PostMapping("/classes")
    public ResponseEntity<Map<String, Object>> addClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String className = (String) request.get("className");
            String baseIri = (String) request.getOrDefault("baseIri", "http://example.com/ontology#");

            if (repoId == null || className == null || className.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "repoId and className are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            String fullIri = (baseIri.endsWith("#") || baseIri.endsWith("/") ? baseIri : baseIri + "#") + className.trim();
            OWLClass newClass = df.getOWLClass(IRI.create(fullIri));

            if (ontology.containsClassInSignature(newClass.getIRI())) {
                response.put("success", false);
                response.put("error", "Class already exists: " + fullIri);
                return ResponseEntity.badRequest().body(response);
            }

            manager.addAxiom(ontology, df.getOWLDeclarationAxiom(newClass));

            @SuppressWarnings("unchecked")
            List<String> superClasses = (List<String>) request.get("superClasses");
            if (superClasses != null && !superClasses.isEmpty()) {
                for (String supIriStr : superClasses) {
                    OWLClass supClass = df.getOWLClass(IRI.create(supIriStr.trim()));
                    manager.addAxiom(ontology, df.getOWLSubClassOfAxiom(newClass, supClass));
                }
            } else {
                manager.addAxiom(ontology, df.getOWLSubClassOfAxiom(newClass, df.getOWLThing()));
            }

            String label = (String) request.get("label");
            if (label != null && !label.trim().isEmpty()) {
                manager.addAxiom(ontology, df.getOWLAnnotationAssertionAxiom(
                        df.getRDFSLabel(), newClass.getIRI(), df.getOWLLiteral(label.trim())));
            }
            String comment = (String) request.get("comment");
            if (comment != null && !comment.trim().isEmpty()) {
                manager.addAxiom(ontology, df.getOWLAnnotationAssertionAxiom(
                        df.getRDFSComment(), newClass.getIRI(), df.getOWLLiteral(comment.trim())));
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            logger.info("Added class: {} to repo: {}", fullIri, repoId);
            response.put("success", true);
            response.put("message", "Class added: " + fullIri);
            response.put("classIri", fullIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding class", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/classes")
    public ResponseEntity<Map<String, Object>> deleteClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String classIri = (String) request.get("classIri");

            if (repoId == null || classIri == null) {
                response.put("success", false);
                response.put("error", "repoId and classIri are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLClass cls = manager.getOWLDataFactory().getOWLClass(IRI.create(classIri));

            // 删除所有相关公理
            for (OWLSubClassOfAxiom ax : getSubClassAxioms(ontology, cls)) {
                manager.removeAxiom(ontology, ax);
            }
            for (OWLEquivalentClassesAxiom ax : getEquivClassAxioms(ontology, cls)) {
                manager.removeAxiom(ontology, ax);
            }
            for (OWLDisjointClassesAxiom ax : ontology.getDisjointClassesAxioms(cls)) {
                manager.removeAxiom(ontology, ax);
            }
            for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(cls)) {
                manager.removeAxiom(ontology, ax);
            }
            for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
                manager.removeAxiom(ontology, ax);
            }
            manager.removeAxiom(ontology, manager.getOWLDataFactory().getOWLDeclarationAxiom(cls));

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            logger.info("Deleted class: {} from repo: {}", classIri, repoId);
            response.put("success", true);
            response.put("message", "Class deleted: " + classIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting class", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private OWLDataFactory df(OWLOntologyManager manager) {
        return manager.getOWLDataFactory();
    }

    @PutMapping("/classes")
    public ResponseEntity<Map<String, Object>> updateClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String classIri = (String) request.get("classIri");
            @SuppressWarnings("unchecked")
            List<String> superClasses = (List<String>) request.get("superClasses");
            @SuppressWarnings("unchecked")
            List<String> equivClasses = (List<String>) request.get("equivalentClasses");
            String comment = (String) request.get("comment");

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLClass cls = df.getOWLClass(IRI.create(classIri));

            // 更新父类
            for (OWLSubClassOfAxiom ax : getSubClassAxioms(ontology, cls)) {
                manager.removeAxiom(ontology, ax);
            }
            if (superClasses != null && !superClasses.isEmpty()) {
                for (String supIriStr : superClasses) {
                    if (!supIriStr.trim().isEmpty()) {
                        manager.addAxiom(ontology, df.getOWLSubClassOfAxiom(cls, df.getOWLClass(IRI.create(supIriStr.trim()))));
                    }
                }
            } else {
                manager.addAxiom(ontology, df.getOWLSubClassOfAxiom(cls, df.getOWLThing()));
            }

            // 更新等价类
            for (OWLEquivalentClassesAxiom ax : getEquivClassAxioms(ontology, cls)) {
                manager.removeAxiom(ontology, ax);
            }
            if (equivClasses != null) {
                for (String eqIriStr : equivClasses) {
                    if (!eqIriStr.trim().isEmpty()) {
                        manager.addAxiom(ontology, df.getOWLEquivalentClassesAxiom(cls, df.getOWLClass(IRI.create(eqIriStr.trim()))));
                    }
                }
            }

            // 更新注释
            for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
                if (ax.getProperty().isComment()) {
                    manager.removeAxiom(ontology, ax);
                }
            }
            if (comment != null && !comment.trim().isEmpty()) {
                manager.addAxiom(ontology, df.getOWLAnnotationAssertionAxiom(df.getRDFSComment(), cls.getIRI(), df.getOWLLiteral(comment.trim())));
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            logger.info("Updated class: {} in repo: {}", classIri, repoId);
            response.put("success", true);
            response.put("message", "Class updated: " + classIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating class", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 对象属性写入 ==========

    @PostMapping("/objectproperties")
    public ResponseEntity<Map<String, Object>> addObjectProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propName = (String) request.get("propertyName");
            String baseIri = (String) request.getOrDefault("baseIri", "http://example.com/ontology#");
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) request.get("domains");
            @SuppressWarnings("unchecked")
            List<String> ranges = (List<String>) request.get("ranges");
            @SuppressWarnings("unchecked")
            Map<String, Boolean> chars = (Map<String, Boolean>) request.getOrDefault("characteristics", new HashMap<>());
            String comment = (String) request.getOrDefault("comment", null);

            if (repoId == null || propName == null) {
                response.put("success", false);
                response.put("error", "repoId and propertyName are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            String fullIri = (baseIri.endsWith("#") || baseIri.endsWith("/") ? baseIri : baseIri + "#") + propName.trim();
            OWLObjectProperty op = df.getOWLObjectProperty(IRI.create(fullIri));
            manager.addAxiom(ontology, df.getOWLDeclarationAxiom(op));

            if (domains != null) {
                for (String domIri : domains) {
                    if (!domIri.trim().isEmpty()) {
                        manager.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, df.getOWLClass(IRI.create(domIri.trim()))));
                    }
                }
            }
            if (ranges != null) {
                for (String ranIri : ranges) {
                    if (!ranIri.trim().isEmpty()) {
                        manager.addAxiom(ontology, df.getOWLObjectPropertyRangeAxiom(op, df.getOWLClass(IRI.create(ranIri.trim()))));
                    }
                }
            }
            if (Boolean.TRUE.equals(chars.get("functional"))) manager.addAxiom(ontology, df.getOWLFunctionalObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("transitive"))) manager.addAxiom(ontology, df.getOWLTransitiveObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("symmetric"))) manager.addAxiom(ontology, df.getOWLSymmetricObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("inverseFunctional"))) manager.addAxiom(ontology, df.getOWLInverseFunctionalObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("asymmetric"))) manager.addAxiom(ontology, df.getOWLAsymmetricObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("reflexive"))) manager.addAxiom(ontology, df.getOWLReflexiveObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(chars.get("irreflexive"))) manager.addAxiom(ontology, df.getOWLIrreflexiveObjectPropertyAxiom(op));
            if (comment != null && !comment.trim().isEmpty()) {
                manager.addAxiom(ontology, df.getOWLAnnotationAssertionAxiom(
                        df.getRDFSComment(), op.getIRI(), df.getOWLLiteral(comment.trim())));
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            logger.info("Added object property: {} to repo: {}", fullIri, repoId);
            response.put("success", true);
            response.put("message", "Object property added: " + fullIri);
            response.put("propertyIri", fullIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding object property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/objectproperties")
    public ResponseEntity<Map<String, Object>> deleteObjectProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propIri = (String) request.get("propertyIri");

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLObjectProperty op = manager.getOWLDataFactory().getOWLObjectProperty(IRI.create(propIri));

            for (OWLObjectPropertyDomainAxiom ax : getOPDomainAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLObjectPropertyRangeAxiom ax : getOPRangeAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLSubObjectPropertyOfAxiom ax : getSubOPAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLInverseObjectPropertiesAxiom ax : getInvOPAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLFunctionalObjectPropertyAxiom ax : ontology.getFunctionalObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLTransitiveObjectPropertyAxiom ax : ontology.getTransitiveObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLSymmetricObjectPropertyAxiom ax : ontology.getSymmetricObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLDeclarationAxiom ax : ontology.getDeclarationAxioms(op)) manager.removeAxiom(ontology, ax);

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Object property deleted: " + propIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting object property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 对象属性写入：编辑（重建所有 axiom） ==========

    @PutMapping("/objectproperties")
    public ResponseEntity<Map<String, Object>> updateObjectProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propIri = (String) request.get("propertyIri");
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) request.get("domains");
            @SuppressWarnings("unchecked")
            List<String> ranges = (List<String>) request.get("ranges");
            @SuppressWarnings("unchecked")
            List<String> superProperties = (List<String>) request.get("superProperties");
            String inverseProperty = (String) request.getOrDefault("inverseProperty", null);
            @SuppressWarnings("unchecked")
            Map<String, Boolean> characteristics = (Map<String, Boolean>) request.getOrDefault("characteristics", new HashMap<>());

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLObjectProperty op = df.getOWLObjectProperty(IRI.create(propIri));

            // 移除旧的关联 axiom（保留 Declaration）
            for (OWLObjectPropertyDomainAxiom ax : getOPDomainAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLObjectPropertyRangeAxiom ax : getOPRangeAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLSubObjectPropertyOfAxiom ax : getSubOPAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLInverseObjectPropertiesAxiom ax : getInvOPAxioms(ontology, op)) manager.removeAxiom(ontology, ax);
            for (OWLFunctionalObjectPropertyAxiom ax : ontology.getFunctionalObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLTransitiveObjectPropertyAxiom ax : ontology.getTransitiveObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLSymmetricObjectPropertyAxiom ax : ontology.getSymmetricObjectPropertyAxioms(op)) manager.removeAxiom(ontology, ax);
            for (OWLInverseFunctionalObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) manager.removeAxiom(ontology, ax);
            }
            for (OWLAsymmetricObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.ASYMMETRIC_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) manager.removeAxiom(ontology, ax);
            }
            for (OWLReflexiveObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.REFLEXIVE_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) manager.removeAxiom(ontology, ax);
            }
            for (OWLIrreflexiveObjectPropertyAxiom ax : ontology.getAxioms(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY)) {
                if (op.equals(ax.getProperty())) manager.removeAxiom(ontology, ax);
            }

            // 添加新的 Domain
            if (domains != null) {
                for (String d : domains) {
                    manager.addAxiom(ontology, df.getOWLObjectPropertyDomainAxiom(op, df.getOWLClass(IRI.create(d))));
                }
            }
            // 添加新的 Range
            if (ranges != null) {
                for (String r : ranges) {
                    manager.addAxiom(ontology, df.getOWLObjectPropertyRangeAxiom(op, df.getOWLClass(IRI.create(r))));
                }
            }
            // 添加新的父属性
            if (superProperties != null) {
                for (String sp : superProperties) {
                    manager.addAxiom(ontology, df.getOWLSubObjectPropertyOfAxiom(op, df.getOWLObjectProperty(IRI.create(sp))));
                }
            }
            // 添加逆属性
            if (inverseProperty != null && !inverseProperty.isEmpty()) {
                manager.addAxiom(ontology, df.getOWLInverseObjectPropertiesAxiom(op, df.getOWLObjectProperty(IRI.create(inverseProperty))));
            }
            // 添加特性
            if (Boolean.TRUE.equals(characteristics.get("functional")))
                manager.addAxiom(ontology, df.getOWLFunctionalObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("transitive")))
                manager.addAxiom(ontology, df.getOWLTransitiveObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("symmetric")))
                manager.addAxiom(ontology, df.getOWLSymmetricObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("inverseFunctional")))
                manager.addAxiom(ontology, df.getOWLInverseFunctionalObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("asymmetric")))
                manager.addAxiom(ontology, df.getOWLAsymmetricObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("reflexive")))
                manager.addAxiom(ontology, df.getOWLReflexiveObjectPropertyAxiom(op));
            if (Boolean.TRUE.equals(characteristics.get("irreflexive")))
                manager.addAxiom(ontology, df.getOWLIrreflexiveObjectPropertyAxiom(op));

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Object property updated: " + propIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating object property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 数据属性写入 ==========

    @PostMapping("/dataproperties")
    public ResponseEntity<Map<String, Object>> addDataProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propName = (String) request.get("propertyName");
            String baseIri = (String) request.getOrDefault("baseIri", "http://example.com/ontology#");
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) request.get("domains");
            String range = (String) request.getOrDefault("range", "http://www.w3.org/2001/XMLSchema#string");
            Boolean functional = (Boolean) request.getOrDefault("functional", true);
            String comment = (String) request.getOrDefault("comment", null);

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            String fullIri = (baseIri.endsWith("#") || baseIri.endsWith("/") ? baseIri : baseIri + "#") + propName.trim();
            OWLDataProperty dp = df.getOWLDataProperty(IRI.create(fullIri));
            manager.addAxiom(ontology, df.getOWLDeclarationAxiom(dp));

            if (domains != null) {
                for (String domIri : domains) {
                    if (!domIri.trim().isEmpty()) {
                        manager.addAxiom(ontology, df.getOWLDataPropertyDomainAxiom(dp, df.getOWLClass(IRI.create(domIri.trim()))));
                    }
                }
            }
            manager.addAxiom(ontology, df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatype(IRI.create(range))));
            if (Boolean.TRUE.equals(functional)) {
                manager.addAxiom(ontology, df.getOWLFunctionalDataPropertyAxiom(dp));
            }
            if (comment != null && !comment.trim().isEmpty()) {
                manager.addAxiom(ontology, df.getOWLAnnotationAssertionAxiom(
                        df.getRDFSComment(), dp.getIRI(), df.getOWLLiteral(comment.trim())));
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            logger.info("Added data property: {} to repo: {}", fullIri, repoId);
            response.put("success", true);
            response.put("message", "Data property added: " + fullIri);
            response.put("propertyIri", fullIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding data property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 数据属性写入：编辑 ==========

    @PutMapping("/dataproperties")
    public ResponseEntity<Map<String, Object>> updateDataProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propIri = (String) request.get("propertyIri");
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) request.get("domains");
            String range = (String) request.get("range");
            Boolean functional = (Boolean) request.get("functional");

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLDataProperty dp = df.getOWLDataProperty(IRI.create(propIri));

            // 移除旧的 axiom
            for (OWLDataPropertyDomainAxiom ax : ontology.getDataPropertyDomainAxioms(dp)) manager.removeAxiom(ontology, ax);
            for (OWLDataPropertyRangeAxiom ax : ontology.getDataPropertyRangeAxioms(dp)) manager.removeAxiom(ontology, ax);
            for (OWLFunctionalDataPropertyAxiom ax : ontology.getFunctionalDataPropertyAxioms(dp)) manager.removeAxiom(ontology, ax);

            // 添加新的 Domain
            if (domains != null) {
                for (String d : domains) {
                    manager.addAxiom(ontology, df.getOWLDataPropertyDomainAxiom(dp, df.getOWLClass(IRI.create(d))));
                }
            }
            // 添加新的 Range
            if (range != null && !range.isEmpty()) {
                manager.addAxiom(ontology, df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatype(IRI.create(range))));
            }
            // 添加 Functional
            if (Boolean.TRUE.equals(functional)) {
                manager.addAxiom(ontology, df.getOWLFunctionalDataPropertyAxiom(dp));
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Data property updated: " + propIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating data property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/dataproperties")
    public ResponseEntity<Map<String, Object>> deleteDataProperty(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String propIri = (String) request.get("propertyIri");

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataProperty dp = manager.getOWLDataFactory().getOWLDataProperty(IRI.create(propIri));

            for (OWLDataPropertyDomainAxiom ax : ontology.getDataPropertyDomainAxioms(dp)) manager.removeAxiom(ontology, ax);
            for (OWLDataPropertyRangeAxiom ax : ontology.getDataPropertyRangeAxioms(dp)) manager.removeAxiom(ontology, ax);
            for (OWLFunctionalDataPropertyAxiom ax : ontology.getFunctionalDataPropertyAxioms(dp)) manager.removeAxiom(ontology, ax);
            for (OWLDeclarationAxiom ax : ontology.getDeclarationAxioms(dp)) manager.removeAxiom(ontology, ax);

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Data property deleted: " + propIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting data property", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== Disjoint Classes ==========

    @GetMapping("/disjoint")
    public ResponseEntity<Map<String, Object>> getDisjointClasses(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = readBaseIri(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");
            List<Map<String, Object>> entries = new ArrayList<>();

            for (OWLDisjointClassesAxiom ax : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                List<OWLClass> classes = ax.getClassesInSignature().stream().collect(java.util.stream.Collectors.toList());
                if (classes.size() < 2) continue;
                Map<String, Object> entry = new HashMap<>();
                List<String> iris = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (OWLClass cls : classes) {
                    String iri = cls.getIRI().toString();
                    iris.add(iri);
                    String shortForm = extractPrefix(iri, baseIri);
                    labels.add(shortForm);
                }
                entry.put("classes", iris);
                entry.put("labels", labels);
                entries.add(entry);
            }

            response.put("success", true);
            response.put("disjoints", entries);
            response.put("total", entries.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting disjoint classes", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/disjoint")
    public ResponseEntity<Map<String, Object>> addDisjointClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            @SuppressWarnings("unchecked")
            List<String> classIris = (List<String>) request.get("classIris");

            if (repoId == null || classIris == null || classIris.size() < 2) {
                response.put("success", false);
                response.put("error", "repoId and at least 2 classIris are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            List<OWLClass> classes = new ArrayList<>();
            for (String iri : classIris) {
                classes.add(df.getOWLClass(IRI.create(iri.trim())));
            }

            OWLDisjointClassesAxiom ax = df.getOWLDisjointClassesAxiom(classes);
            manager.addAxiom(ontology, ax);
            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));

            response.put("success", true);
            response.put("message", "Disjoint classes added: " + classIris);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding disjoint classes", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/disjoint")
    public ResponseEntity<Map<String, Object>> removeDisjointClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            @SuppressWarnings("unchecked")
            List<String> classIris = (List<String>) request.get("classIris");

            if (repoId == null || classIris == null || classIris.size() < 2) {
                response.put("success", false);
                response.put("error", "repoId and at least 2 classIris are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            // 找到完全匹配的 axiom 并删除
            List<OWLClass> targetClasses = new ArrayList<>();
            for (String iri : classIris) {
                targetClasses.add(df.getOWLClass(IRI.create(iri.trim())));
            }
            final List<OWLClass> target = targetClasses;
            for (OWLDisjointClassesAxiom ax : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                List<OWLClass> axClasses = ax.getClassesInSignature().stream().collect(java.util.stream.Collectors.toList());
                if (axClasses.size() == target.size() && new java.util.HashSet<>(axClasses).equals(new java.util.HashSet<>(target))) {
                    manager.removeAxiom(ontology, ax);
                    break;
                }
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Disjoint classes removed: " + classIris);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error removing disjoint classes", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 类层级树 ==========

    @GetMapping("/hierarchy")
    public ResponseEntity<Map<String, Object>> getClassHierarchy(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = readBaseIri(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");

            // 构建 "子类 → 父类" 映射
            Map<String, List<String>> childToParents = new LinkedHashMap<>();
            for (OWLClass cls : ontology.getClassesInSignature()) {
                String iri = cls.getIRI().toString();
                if (iri.startsWith("http://www.w3.org/")) continue;
                childToParents.put(iri, new ArrayList<>());
            }
            for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                OWLClass sub = ax.getSubClass().asOWLClass();
                OWLClass sup = ax.getSuperClass().asOWLClass();
                String subIri = sub.getIRI().toString();
                String supIri = sup.getIRI().toString();
                if (childToParents.containsKey(subIri) && !sup.isOWLThing()) {
                    childToParents.get(subIri).add(supIri);
                }
            }

            // 读取每个类的 label / shortName
            Map<String, String> labelMap = new HashMap<>();
            for (OWLClass cls : ontology.getClassesInSignature()) {
                String iri = cls.getIRI().toString();
                if (iri.startsWith("http://www.w3.org/")) continue;
                String label = cls.getIRI().getShortForm();
                for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
                    IRI propIri = ax.getProperty().getIRI();
                    if (propIri.getRemainder().isPresent() && propIri.getRemainder().get().equals("label")) {
                        OWLLiteral lit = ax.getValue().asLiteral().orElse(null);
                        if (lit != null) label = lit.getLiteral();
                    }
                }
                labelMap.put(iri, label);
            }

            // 构建树节点
            List<Map<String, Object>> nodes = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : childToParents.entrySet()) {
                String iri = entry.getKey();
                List<String> parents = entry.getValue();
                Map<String, Object> node = new HashMap<>();
                node.put("iri", iri);
                node.put("label", labelMap.getOrDefault(iri, iri));
                node.put("parentIris", parents);
                node.put("isTop", parents.isEmpty());
                nodes.add(node);
            }

            response.put("success", true);
            response.put("baseIri", baseIri);
            response.put("nodes", nodes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting class hierarchy", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 本体一致性检查 ==========

    @GetMapping("/consistency")
    public ResponseEntity<Map<String, Object>> checkConsistency(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            if (!owlFile.exists()) {
                response.put("success", false);
                response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            // 尝试找内置 reasoner
            // OWLAPI 5.1.x 自带 StructuralReasoner（不完整推理，仅做基础结构验证）
            // 这里做结构验证：检查重复定义、等价类环等
            List<String> warnings = new ArrayList<>();
            List<String> infos = new ArrayList<>();

            int classCount = 0;
            int opCount = 0;
            int dpCount = 0;
            int equivCount = 0;
            int disjointCount = 0;
            int subClassCount = 0;
            int totalAxioms = ontology.getAxiomCount();

            for (OWLClass cls : ontology.getClassesInSignature()) {
                if (!cls.getIRI().toString().startsWith("http://www.w3.org/")) classCount++;
            }
            for (OWLObjectProperty op : ontology.getObjectPropertiesInSignature()) {
                if (!op.getIRI().toString().startsWith("http://www.w3.org/")) opCount++;
            }
            for (OWLDataProperty dp : ontology.getDataPropertiesInSignature()) {
                if (!dp.getIRI().toString().startsWith("http://www.w3.org/")) dpCount++;
            }
            for (OWLEquivalentClassesAxiom ax : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) equivCount++;
            for (OWLDisjointClassesAxiom ax : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) disjointCount++;
            for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) subClassCount++;

            // 检查等价类环（自己等价自己）
            for (OWLEquivalentClassesAxiom ax : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
                for (OWLClass c : ax.getClassesInSignature()) {
                    if (ax.getClassesInSignature().size() == 1 ||
                        !ax.getClassesInSignature().stream().filter(x -> !x.equals(c)).findFirst().isPresent()) {
                        warnings.add("等价类定义异常: " + c.getIRI().getShortForm());
                    }
                }
            }

            // 检查互斥类是否有交集（粗略检查：等价类和互斥不能共存）
            Set<String> equivClasses = new HashSet<>();
            for (OWLEquivalentClassesAxiom ax : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
                for (OWLClass c : ax.getClassesInSignature()) {
                    equivClasses.add(c.getIRI().toString());
                }
            }
            for (OWLDisjointClassesAxiom dax : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                Set<String> dClasses = new HashSet<>();
                for (OWLClass c : dax.getClassesInSignature()) {
                    dClasses.add(c.getIRI().toString());
                }
                dClasses.retainAll(equivClasses);
                if (!dClasses.isEmpty()) {
                    warnings.add("类同时出现在等价组和互斥组中，可能导致不一致: " + dClasses);
                }
            }

            infos.add("本体结构验证通过");
            infos.add("类: " + classCount + " | 对象属性: " + opCount + " | 数据属性: " + dpCount);
            infos.add("SubClassOf 公理: " + subClassCount + " | 等价类: " + equivCount + " | 互斥类: " + disjointCount);
            infos.add("总 axiom 数: " + totalAxioms);

            response.put("success", true);
            response.put("consistent", warnings.isEmpty());
            response.put("warnings", warnings);
            response.put("infos", infos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking consistency", e);
            response.put("success", false);
            response.put("consistent", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== Annotation 增删改 ==========

    @PutMapping("/annotations")
    public ResponseEntity<Map<String, Object>> updateAnnotations(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String entityIri = (String) request.get("entityIri");
            String label = (String) request.getOrDefault("label", null);
            String comment = (String) request.getOrDefault("comment", null);

            if (repoId == null || entityIri == null) {
                response.put("success", false);
                response.put("error", "repoId and entityIri are required");
                return ResponseEntity.badRequest().body(response);
            }

            String owlPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl";
            File owlFile = new File(owlPath);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLNamedObject entity = df.getOWLClass(IRI.create(entityIri));

            // 先尝试确定实体类型
            OWLEntity ent = findEntity(ontology, IRI.create(entityIri));
            if (ent == null) {
                response.put("success", false);
                response.put("error", "Entity not found in ontology: " + entityIri);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 删除旧的 label 和 comment 注解
            IRI subject = ent.getIRI();
            for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(subject)) {
                IRI propIri = ax.getProperty().getIRI();
                String local = propIri.getRemainder().orElse("");
                if (local.equals("label") || local.equals("comment")) {
                    manager.removeAxiom(ontology, ax);
                }
            }
            // 添加新的 label
            if (label != null && !label.trim().isEmpty()) {
                OWLLiteral labelLit = df.getOWLLiteral(label.trim());
                OWLAnnotationAssertionAxiom labelAx = df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), subject, labelLit);
                manager.addAxiom(ontology, labelAx);
            }
            // 添加新的 comment
            if (comment != null && !comment.trim().isEmpty()) {
                OWLLiteral commentLit = df.getOWLLiteral(comment.trim());
                OWLAnnotationAssertionAxiom commentAx = df.getOWLAnnotationAssertionAxiom(df.getRDFSComment(), subject, commentLit);
                manager.addAxiom(ontology, commentAx);
            }

            manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(owlFile));
            response.put("success", true);
            response.put("message", "Annotations updated for: " + entityIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating annotations", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 查找给定 IRI 对应的 OWLEntity（Class / OP / DP）
    private OWLEntity findEntity(OWLOntology ontology, IRI iri) {
        for (OWLClass cls : ontology.getClassesInSignature()) {
            if (cls.getIRI().equals(iri)) return cls;
        }
        for (OWLObjectProperty op : ontology.getObjectPropertiesInSignature()) {
            if (op.getIRI().equals(iri)) return op;
        }
        for (OWLDataProperty dp : ontology.getDataPropertiesInSignature()) {
            if (dp.getIRI().equals(iri)) return dp;
        }
        return null;
    }

    // ========== OBDA 映射追加 ==========

    @PostMapping("/obda/mappings")
    public ResponseEntity<Map<String, Object>> appendObdaMapping(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String mappingId = (String) request.get("mappingId");
            String targetSubject = (String) request.get("targetSubject");
            String targetPredicate = (String) request.get("targetPredicate");
            String targetObject = (String) request.get("targetObject");
            String sourceQuery = (String) request.get("sourceQuery");
            String mappingType = (String) request.getOrDefault("mappingType", "dataProperty");

            if (repoId == null || mappingId == null || targetSubject == null || sourceQuery == null) {
                response.put("success", false);
                response.put("error", "repoId, mappingId, targetSubject, and sourceQuery are required");
                return ResponseEntity.badRequest().body(response);
            }

            String obdaPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/mapping.obda";
            File obdaFile = new File(obdaPath);
            if (!obdaFile.exists()) {
                response.put("success", false);
                response.put("error", "OBDA file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String content = new String(Files.readAllBytes(obdaFile.toPath()), StandardCharsets.UTF_8);
            int insertPos = content.lastIndexOf("]]");
            if (insertPos < 0) {
                response.put("success", false);
                response.put("error", "Invalid OBDA file format: missing closing ']]'");
                return ResponseEntity.badRequest().body(response);
            }

            StringBuilder mappingBlock = new StringBuilder();
            mappingBlock.append("\nmappingId\t").append(mappingId.trim()).append("\n");
            mappingBlock.append("target\t\t").append(targetSubject.trim());

            if ("objectProperty".equals(mappingType)) {
                mappingBlock.append(" ").append(targetPredicate.trim()).append(" ").append(targetObject.trim()).append(" .\n");
            } else if ("class".equals(mappingType)) {
                String className = targetSubject.trim();
                if (className.startsWith(":")) className = className.substring(1);
                int slashIdx = className.indexOf('/');
                if (slashIdx > 0) className = className.substring(0, slashIdx);
                mappingBlock.append(" a :").append(className).append(" .\n");
            } else {
                String pred = targetPredicate != null && !targetPredicate.trim().isEmpty() ? " " + targetPredicate.trim() : "";
                String obj = targetObject != null && !targetObject.trim().isEmpty() ? " {" + targetObject.trim() + "}" : "";
                mappingBlock.append(pred).append(obj).append(" .\n");
            }
            mappingBlock.append("source\t\t").append(sourceQuery.trim()).append("\n\n");

            String newContent = content.substring(0, insertPos) + mappingBlock + content.substring(insertPos);
            try (FileWriter writer = new FileWriter(obdaFile)) {
                writer.write(newContent);
            }

            logger.info("Appended OBDA mapping: {} to repo: {}", mappingId, repoId);
            response.put("success", true);
            response.put("message", "OBDA mapping appended: " + mappingId);
            response.put("mappingId", mappingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error appending OBDA mapping", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/obda/mappings")
    public ResponseEntity<Map<String, Object>> getObdaMappings(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String obdaPath = System.getProperty("user.dir") + "/uploads/" + repoId + "/mapping.obda";
            File obdaFile = new File(obdaPath);
            if (!obdaFile.exists()) {
                response.put("success", false);
                response.put("error", "OBDA file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String content = new String(Files.readAllBytes(obdaFile.toPath()), StandardCharsets.UTF_8);
            List<Map<String, Object>> mappings = new ArrayList<>();
            String[] lines = content.split("\n");
            Map<String, Object> current = null;
            String currentSection = null;
            StringBuilder sourceBlock = new StringBuilder();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("mappingId")) {
                    if (current != null && sourceBlock.length() > 0) {
                        current.put("source", sourceBlock.toString().trim());
                        mappings.add(current);
                    }
                    current = new HashMap<>();
                    current.put("mappingId", trimmed.substring("mappingId".length()).trim());
                    currentSection = "mappingId";
                    sourceBlock = new StringBuilder();
                } else if (trimmed.startsWith("target")) {
                    if (current != null) current.put("target", trimmed.substring("target".length()).trim());
                    currentSection = "target";
                } else if (trimmed.startsWith("source")) {
                    currentSection = "source";
                } else if (!trimmed.isEmpty() && "source".equals(currentSection)) {
                    sourceBlock.append(trimmed).append(" ");
                }
            }
            if (current != null && sourceBlock.length() > 0) {
                current.put("source", sourceBlock.toString().trim());
                mappings.add(current);
            }

            response.put("success", true);
            response.put("repoId", repoId);
            response.put("mappings", mappings);
            response.put("total", mappings.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting OBDA mappings", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 上传新本体文件（覆盖更新仓库） ==========

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadOntologyFiles(
            @RequestParam("repoId") String repoId,
            @RequestParam(value = "owlFile", required = false) MultipartFile owlFile,
            @RequestParam(value = "obdaFile", required = false) MultipartFile obdaFile,
            @RequestParam(value = "autoRestart", defaultValue = "true") boolean autoRestart) {
        Map<String, Object> response = new HashMap<>();
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/" + repoId + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            boolean anyUploaded = false;
            StringBuilder msgs = new StringBuilder();

            if (owlFile != null && !owlFile.isEmpty()) {
                owlFile.transferTo(new File(uploadDir + "ontology.owl"));
                msgs.append("OWL文件已更新;");
                anyUploaded = true;
            }
            if (obdaFile != null && !obdaFile.isEmpty()) {
                obdaFile.transferTo(new File(uploadDir + "mapping.obda"));
                msgs.append("OBDA文件已更新;");
                anyUploaded = true;
            }

            if (!anyUploaded) {
                response.put("success", false);
                response.put("error", "No file provided");
                return ResponseEntity.badRequest().body(response);
            }

            if (autoRestart) {
                try {
                    String restartUrl = props.getGraphDb().getUrl() + "/rest/repositories/" + repoId + "/restart";
                    new org.springframework.web.client.RestTemplate().postForEntity(restartUrl, null, String.class);
                    msgs.append("仓库已自动重启。");
                } catch (Exception re) {
                    msgs.append("文件已更新，但自动重启失败: ").append(re.getMessage()).append("。请手动重启。");
                }
            } else {
                msgs.append("文件已更新，可手动重启仓库使更改生效。");
            }

            response.put("success", true);
            response.put("repoId", repoId);
            response.put("message", msgs.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading ontology files for repo: {}", repoId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 保存并热重启仓库 ==========

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyAndRestart(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String restartUrl = props.getGraphDb().getUrl() + "/rest/repositories/" + repoId + "/restart";
            org.springframework.http.ResponseEntity<String> resp =
                    new org.springframework.web.client.RestTemplate().postForEntity(restartUrl, null, String.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                logger.info("Repository {} restarted after ontology update", repoId);
                response.put("success", true);
                response.put("message", "Changes saved and repository restarted successfully");
                response.put("repoId", repoId);
            } else {
                response.put("success", false);
                response.put("message", "Repository restart returned: " + resp.getStatusCode());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error restarting repository after apply", e);
            response.put("success", false);
            response.put("message", "Restart failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
