package com.example.owlapi.api;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.io.FileDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * OWL 本体编辑器 - 高级功能 API
 *
 * 功能：
 * 1. Individuals (个体/实例) CRUD + SameAs/DifferentFrom
 * 2. DL Query (推理查询: super/sub classes + instances)
 * 3. 多格式导出 (RDF/XML, Turtle, OWL/XML, Manchester, JSON-LD)
 * 4. Prefix / 命名空间管理
 * 5. 本体导入 (OWLImportsManager)
 * 6. 本体比较与合并
 * 7. 撤销 / 重做
 * 8. Property Chains (属性链 SubPropertyChainOf)
 * 9. 等价类声明管理
 * 10. OBDA 映射增删
 */
@RestController
@RequestMapping("/api/ontology/advanced")
public class OntologyEditorAdvancedController {

    private static final Logger logger = LoggerFactory.getLogger(OntologyEditorAdvancedController.class);
    private final SystemBuiltinProperties props;

    /** 仓库级别的变更历史栈 */
    private final Map<String, Deque<Set<OWLOntologyChange>>> undoStack = new HashMap<>();
    private final Map<String, Deque<Set<OWLOntologyChange>>> redoStack = new HashMap<>();
    private static final int MAX_HISTORY = 50;

    /** 推理机缓存 */
    private final Map<String, OWLReasoner> reasonerCache = new HashMap<>();

    public OntologyEditorAdvancedController(SystemBuiltinProperties props) {
        this.props = props;
    }

    // ==================== 辅助方法 ====================

    private File getOwlFile(String repoId) {
        return new File(System.getProperty("user.dir") + "/uploads/" + repoId + "/ontology.owl");
    }

    private File getObdaFile(String repoId) {
        return new File(System.getProperty("user.dir") + "/uploads/" + repoId + "/mapping.obda");
    }

    private String getBaseIri(String repoId) {
        try {
            File propsFile = new File(System.getProperty("user.dir") + "/uploads/" + repoId + "/config.properties");
            if (!propsFile.exists()) return "http://example.com/ontology#";
            String content = new String(Files.readAllBytes(propsFile.toPath()), StandardCharsets.UTF_8);
            for (String line : content.split("\n")) {
                if (line.startsWith("base.iri=")) return line.substring("base.iri=".length()).trim();
            }
        } catch (Exception ignored) {}
        return "http://example.com/ontology#";
    }

    private String extractPrefix(String iri, String baseIri) {
        if (baseIri != null && iri.startsWith(baseIri)) return iri.substring(baseIri.length());
        return iri;
    }

    private String shortLabel(OWLClass c, String baseIri) {
        if (c == null) return "";
        return extractPrefix(c.getIRI().toString(), baseIri);
    }

    private String shortLabel(OWLNamedIndividual ind, String baseIri) {
        if (ind == null) return "";
        return extractPrefix(ind.getIRI().toString(), baseIri);
    }

    private String shortLabel(OWLObjectProperty op, String baseIri) {
        if (op == null) return "";
        return extractPrefix(op.getIRI().toString(), baseIri);
    }

    private String shortLabel(OWLDataProperty dp, String baseIri) {
        if (dp == null) return "";
        return extractPrefix(dp.getIRI().toString(), baseIri);
    }

    private void invalidateCache(String repoId) {
        reasonerCache.remove(repoId);
    }

    private void pushUndo(String repoId, Set<OWLOntologyChange> changes) {
        if (changes == null || changes.isEmpty()) return;
        Deque<Set<OWLOntologyChange>> stack = undoStack.computeIfAbsent(repoId, k -> new ArrayDeque<>());
        stack.push(new HashSet<>(changes));
        while (stack.size() > MAX_HISTORY) {
            Iterator<Set<OWLOntologyChange>> it = stack.iterator();
            it.next();
            it.remove();
        }
        redoStack.remove(repoId);
    }

    private void saveOntology(OWLOntologyManager manager, OWLOntology ontology, String repoId) throws OWLException {
        manager.saveOntology(ontology, new RDFXMLDocumentFormat(), new FileDocumentTarget(getOwlFile(repoId)));
        invalidateCache(repoId);
    }

    private OWLReasoner getReasoner(OWLOntology ontology) {
        StructuralReasonerFactory srf = new StructuralReasonerFactory();
        return srf.createNonBufferingReasoner(ontology);
    }

    /**
     * 构建 AddAxiom 或 RemoveAxiom 的反向变更。
     * 用于撤销：AddAxiom → RemoveAxiom；RemoveAxiom → AddAxiom。
     */
    private Set<OWLOntologyChange> invertChanges(Set<OWLOntologyChange> changes) {
        Set<OWLOntologyChange> inverted = new HashSet<>();
        for (OWLOntologyChange change : changes) {
            if (change instanceof AddAxiom) {
                inverted.add(new RemoveAxiom(((AddAxiom) change).getOntology(),
                        ((AddAxiom) change).getAxiom()));
            } else if (change instanceof RemoveAxiom) {
                inverted.add(new AddAxiom(((RemoveAxiom) change).getOntology(),
                        ((RemoveAxiom) change).getAxiom()));
            }
        }
        return inverted;
    }

    // ==================== 1. Individuals ====================

    @GetMapping("/individuals")
    public ResponseEntity<Map<String, Object>> getIndividuals(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            if (!owlFile.exists()) {
                response.put("success", false); response.put("error", "OWL file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);

            List<Map<String, Object>> individuals = new ArrayList<>();
            for (OWLNamedIndividual ind : ontology.getIndividualsInSignature()) {
                String iriStr = ind.getIRI().toString();
                if (iriStr.startsWith("http://www.w3.org/")) continue;

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("iri", iriStr);
                info.put("prefix", extractPrefix(iriStr, baseIri));

                List<String> types = new ArrayList<>();
                for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(ind)) {
                    OWLClassExpression ce = ax.getClassExpression();
                    if (!ce.isOWLThing() && ce.isNamed()) {
                        types.add(ce.asOWLClass().getIRI().toString());
                    }
                }
                info.put("types", types);

                List<Map<String, Object>> opAssertions = new ArrayList<>();
                for (OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(ind)) {
                    if (ax.getSubject().equals(ind) && ax.getObject().isNamed()) {
                        Map<String, Object> a = new HashMap<>();
                        a.put("property", ax.getProperty().asOWLObjectProperty().getIRI().toString());
                        a.put("value", ax.getObject().asOWLNamedIndividual().getIRI().toString());
                        opAssertions.add(a);
                    }
                }
                info.put("objectPropertyAssertions", opAssertions);

                List<Map<String, Object>> dpAssertions = new ArrayList<>();
                for (OWLDataPropertyAssertionAxiom ax : ontology.getDataPropertyAssertionAxioms(ind)) {
                    if (ax.getSubject().equals(ind)) {
                        Map<String, Object> a = new HashMap<>();
                        a.put("property", ax.getProperty().asOWLDataProperty().getIRI().toString());
                        a.put("value", ax.getObject().getLiteral());
                        dpAssertions.add(a);
                    }
                }
                info.put("dataPropertyAssertions", dpAssertions);

                individuals.add(info);
            }

            response.put("success", true);
            response.put("baseIri", baseIri);
            response.put("individuals", individuals);
            response.put("total", individuals.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("getIndividuals failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/individuals")
    public ResponseEntity<Map<String, Object>> addIndividual(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String individualName = (String) request.get("individualName");
            String baseIri = (String) request.getOrDefault("baseIri", getBaseIri(repoId));
            @SuppressWarnings("unchecked")
            List<String> classIris = (List<String>) request.get("classTypes");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dpAssertions = (List<Map<String, Object>>) request.get("dataPropertyAssertions");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> opAssertions = (List<Map<String, Object>>) request.get("objectPropertyAssertions");

            if (repoId == null || individualName == null) {
                response.put("success", false); response.put("error", "repoId and individualName required");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            String sep = (baseIri.endsWith("#") || baseIri.endsWith("/")) ? "" : "#";
            String fullIri = baseIri + sep + individualName.trim();
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(fullIri));

            List<OWLOntologyChange> changes = new ArrayList<>();

            OWLAxiom decl = df.getOWLDeclarationAxiom(ind);
            manager.addAxiom(ontology, decl);
            changes.add(new AddAxiom(ontology, decl));

            if (classIris != null) {
                for (String clsIri : classIris) {
                    if (clsIri != null && !clsIri.trim().isEmpty()) {
                        OWLAxiom ax = df.getOWLClassAssertionAxiom(df.getOWLClass(IRI.create(clsIri.trim())), ind);
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                }
            }
            if (dpAssertions != null) {
                for (Map<String, Object> da : dpAssertions) {
                    String propIri = (String) da.get("property");
                    Object valObj = da.get("value");
                    String value = valObj != null ? valObj.toString() : "";
                    if (propIri != null && !propIri.isEmpty()) {
                        OWLAxiom ax = df.getOWLDataPropertyAssertionAxiom(
                                df.getOWLDataProperty(IRI.create(propIri)), ind, df.getOWLLiteral(value));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                }
            }
            if (opAssertions != null) {
                for (Map<String, Object> oa : opAssertions) {
                    String propIri = (String) oa.get("property");
                    String valueIri = (String) oa.get("value");
                    if (propIri != null && valueIri != null) {
                        OWLAxiom ax = df.getOWLObjectPropertyAssertionAxiom(
                                df.getOWLObjectProperty(IRI.create(propIri)),
                                ind, df.getOWLNamedIndividual(IRI.create(valueIri)));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                }
            }

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Individual added: " + fullIri);
            response.put("individualIri", fullIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("addIndividual failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/individuals")
    public ResponseEntity<Map<String, Object>> deleteIndividual(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String individualIri = (String) request.get("individualIri");
            if (repoId == null || individualIri == null) {
                response.put("success", false); response.put("error", "repoId and individualIri required");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(individualIri));

            List<OWLOntologyChange> changes = new ArrayList<>();
            for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(ind)) {
                manager.removeAxiom(ontology, ax);
                changes.add(new RemoveAxiom(ontology, ax));
            }
            for (OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(ind)) {
                if (ax.getSubject().equals(ind) || ax.getObject().equals(ind)) {
                    manager.removeAxiom(ontology, ax);
                    changes.add(new RemoveAxiom(ontology, ax));
                }
            }
            for (OWLDataPropertyAssertionAxiom ax : ontology.getDataPropertyAssertionAxioms(ind)) {
                if (ax.getSubject().equals(ind)) {
                    manager.removeAxiom(ontology, ax);
                    changes.add(new RemoveAxiom(ontology, ax));
                }
            }
            for (OWLSameIndividualAxiom ax : ontology.getSameIndividualAxioms(ind)) {
                manager.removeAxiom(ontology, ax);
                changes.add(new RemoveAxiom(ontology, ax));
            }
            for (OWLDifferentIndividualsAxiom ax : ontology.getAxioms(AxiomType.DIFFERENT_INDIVIDUALS)) {
                if (ax.getIndividuals().contains(ind)) {
                    manager.removeAxiom(ontology, ax);
                    changes.add(new RemoveAxiom(ontology, ax));
                }
            }
            for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(ind.getIRI())) {
                manager.removeAxiom(ontology, ax);
                changes.add(new RemoveAxiom(ontology, ax));
            }
            OWLAxiom decl = df.getOWLDeclarationAxiom(ind);
            manager.removeAxiom(ontology, decl);
            changes.add(new RemoveAxiom(ontology, decl));

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Individual deleted: " + individualIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("deleteIndividual failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/individuals/assertion")
    public ResponseEntity<Map<String, Object>> addIndividualAssertion(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String individualIri = (String) request.get("individualIri");
            String assertionType = (String) request.get("assertionType");
            String classIri = (String) request.get("classIri");
            String propIri = (String) request.get("propertyIri");
            String value = (String) request.get("value");
            String objectIri = (String) request.get("objectIri");

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(individualIri));

            List<OWLOntologyChange> changes = new ArrayList<>();
            switch (assertionType != null ? assertionType : "") {
                case "classType":
                    if (classIri != null && !classIri.isEmpty()) {
                        OWLAxiom ax = df.getOWLClassAssertionAxiom(df.getOWLClass(IRI.create(classIri)), ind);
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                    break;
                case "dataProperty":
                    if (propIri != null && value != null) {
                        OWLAxiom ax = df.getOWLDataPropertyAssertionAxiom(
                                df.getOWLDataProperty(IRI.create(propIri)), ind, df.getOWLLiteral(value));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                    break;
                case "objectProperty":
                    if (propIri != null && objectIri != null) {
                        OWLAxiom ax = df.getOWLObjectPropertyAssertionAxiom(
                                df.getOWLObjectProperty(IRI.create(propIri)),
                                ind, df.getOWLNamedIndividual(IRI.create(objectIri)));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                    break;
                case "sameAs":
                    if (objectIri != null) {
                        OWLAxiom ax = df.getOWLSameIndividualAxiom(ind, df.getOWLNamedIndividual(IRI.create(objectIri)));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                    break;
                case "differentFrom":
                    if (objectIri != null) {
                        OWLAxiom ax = df.getOWLDifferentIndividualsAxiom(ind, df.getOWLNamedIndividual(IRI.create(objectIri)));
                        manager.addAxiom(ontology, ax);
                        changes.add(new AddAxiom(ontology, ax));
                    }
                    break;
            }

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Assertion added for: " + individualIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("addIndividualAssertion failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/individuals/byClass")
    public ResponseEntity<Map<String, Object>> getIndividualsByClass(@RequestParam String repoId, @RequestParam String classIri) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLClass cls = df.getOWLClass(IRI.create(classIri));

            List<Map<String, Object>> individuals = new ArrayList<>();
            for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(cls)) {
                if (ax.getIndividual().isNamed()) {
                    OWLNamedIndividual ind = ax.getIndividual().asOWLNamedIndividual();
                    Map<String, Object> info = new HashMap<>();
                    info.put("iri", ind.getIRI().toString());
                    info.put("prefix", extractPrefix(ind.getIRI().toString(), baseIri));
                    individuals.add(info);
                }
            }

            response.put("success", true);
            response.put("classIri", classIri);
            response.put("individuals", individuals);
            response.put("total", individuals.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 2. DL Query ====================

    @GetMapping("/dlquery/full")
    public ResponseEntity<Map<String, Object>> dlFullQuery(
            @RequestParam String repoId,
            @RequestParam String classExpression,
            @RequestParam(required = false, defaultValue = "false") boolean direct) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);
            OWLReasoner reasoner = getReasoner(ontology);
            OWLDataFactory df = manager.getOWLDataFactory();

            OWLClass targetClass = findClassByName(ontology, classExpression.trim(), baseIri, df);
            if (targetClass == null) {
                response.put("success", false); response.put("error", "Class not found: " + classExpression);
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("rawExpression", classExpression);
            result.put("parsedDescription", targetClass.toString());
            result.put("classIri", targetClass.getIRI().toString());

            result.put("consistent", reasoner.isConsistent());
            result.put("satisfiable", reasoner.isSatisfiable(targetClass));

            // SuperClasses (direct)
            List<String> supers = new ArrayList<>();
            NodeSet<OWLClass> superNodes = reasoner.getSuperClasses(targetClass);
            for (org.semanticweb.owlapi.reasoner.Node<OWLClass> node : superNodes) {
                for (OWLClass c : node) {
                    if (!c.isOWLThing()) supers.add(shortLabel(c, baseIri));
                }
            }
            result.put("superClasses", supers);

            // SubClasses (direct)
            List<String> subs = new ArrayList<>();
            NodeSet<OWLClass> subNodes = reasoner.getSubClasses(targetClass);
            for (org.semanticweb.owlapi.reasoner.Node<OWLClass> node : subNodes) {
                for (OWLClass c : node) {
                    if (!c.isOWLThing()) subs.add(shortLabel(c, baseIri));
                }
            }
            result.put("subClasses", subs);

            // All instances (direct=false → all)
            List<Map<String, Object>> instances = new ArrayList<>();
            NodeSet<OWLNamedIndividual> instNodes = reasoner.getInstances(targetClass);
            for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> node : instNodes) {
                for (OWLNamedIndividual ind2 : node) {
                    Map<String, Object> i = new HashMap<>();
                    i.put("iri", ind2.getIRI().toString());
                    i.put("label", shortLabel(ind2, baseIri));
                    instances.add(i);
                }
            }
            result.put("instances", instances);
            result.put("instanceCount", instances.size());

            response.put("success", true);
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("dlFullQuery failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/dlquery/satisfiable")
    public ResponseEntity<Map<String, Object>> dlSatisfiable(@RequestParam String repoId, @RequestParam String classExpression) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);
            OWLReasoner reasoner = getReasoner(ontology);
            OWLDataFactory df = manager.getOWLDataFactory();

            OWLClass cls = findClassByName(ontology, classExpression.trim(), baseIri, df);
            if (cls == null) {
                response.put("success", false); response.put("error", "Class not found: " + classExpression);
                return ResponseEntity.badRequest().body(response);
            }

            boolean satisfiable = reasoner.isSatisfiable(cls);
            response.put("success", true);
            response.put("classExpression", classExpression);
            response.put("satisfiable", satisfiable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/dlquery/types")
    public ResponseEntity<Map<String, Object>> dlTypes(@RequestParam String repoId, @RequestParam String individualIri) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLReasoner reasoner = getReasoner(ontology);
            OWLDataFactory df = manager.getOWLDataFactory();
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(individualIri));

            List<String> classTypes = new ArrayList<>();
            NodeSet<OWLClass> typeNodes = reasoner.getTypes(ind);
            for (org.semanticweb.owlapi.reasoner.Node<OWLClass> node : typeNodes) {
                for (OWLClass c : node) {
                    if (!c.isOWLThing()) classTypes.add(c.getIRI().toString());
                }
            }
            response.put("success", true);
            response.put("individualIri", individualIri);
            response.put("types", classTypes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 按名称查找本体中的类，支持直接匹配 IRI 前缀匹配。
     */
    private OWLClass findClassByName(OWLOntology ontology, String name, String baseIri, OWLDataFactory df) {
        for (OWLClass c : ontology.getClassesInSignature()) {
            String remainder = c.getIRI().getRemainder().orElse("");
            if (remainder.equalsIgnoreCase(name)) return c;
        }
        String fullIri = (baseIri.endsWith("#") || baseIri.endsWith("/") ? baseIri : baseIri + "#") + name;
        for (OWLClass c : ontology.getClassesInSignature()) {
            if (c.getIRI().toString().equalsIgnoreCase(fullIri)) return c;
        }
        return df.getOWLClass(IRI.create(fullIri));
    }

    // ==================== 3. 多格式导出 ====================

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOntology(
            @RequestParam String repoId,
            @RequestParam(defaultValue = "rdfxml") String format,
            @RequestParam(required = false) String fileName) {
        try {
            File owlFile = getOwlFile(repoId);
            if (!owlFile.exists()) return ResponseEntity.notFound().build();

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            OWLDocumentFormat owlFormat;
            MediaType mediaType;
            String ext;

            switch (format.toLowerCase()) {
                case "turtle": case "ttl":
                    owlFormat = new TurtleDocumentFormat(); mediaType = MediaType.parseMediaType("application/x-turtle"); ext = "ttl"; break;
                case "owlxml": case "owx":
                    owlFormat = new OWLXMLDocumentFormat(); mediaType = MediaType.parseMediaType("application/owl+xml"); ext = "owx"; break;
                case "manchester": case "func": case "functional":
                    owlFormat = new FunctionalSyntaxDocumentFormat(); mediaType = MediaType.parseMediaType("text/plain"); ext = "ofn"; break;
                default:
                    owlFormat = new RDFXMLDocumentFormat(); mediaType = MediaType.APPLICATION_XML; ext = "owl";
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            manager.saveOntology(ontology, owlFormat, baos);

            String name = (fileName != null && !fileName.isEmpty()) ? fileName : "ontology";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(name + "." + ext, StandardCharsets.UTF_8).build());
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/manchester")
    public ResponseEntity<Map<String, Object>> exportManchester(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);

            StringBuilder sb = new StringBuilder();
            sb.append("# Ontology: ").append(ontology.getOntologyID().getOntologyIRI()
                    .map(IRI::toString).orElse("anonymous")).append("\n");
            sb.append("# Manchester Syntax Export\n\n");

            for (OWLClass c : ontology.getClassesInSignature()) {
                if (c.isOWLThing() || c.isOWLNothing()) continue;
                sb.append("Class: ").append(c.getIRI().getRemainder().orElse(c.getIRI().toString())).append("\n");
            }
            for (OWLObjectProperty op : ontology.getObjectPropertiesInSignature()) {
                if (op.isOWLTopObjectProperty() || op.isOWLBottomObjectProperty()) continue;
                sb.append("ObjectProperty: ").append(op.getIRI().getRemainder().orElse(op.getIRI().toString())).append("\n");
            }
            for (OWLDataProperty dp : ontology.getDataPropertiesInSignature()) {
                if (dp.isOWLTopDataProperty() || dp.isOWLBottomDataProperty()) continue;
                sb.append("DataProperty: ").append(dp.getIRI().getRemainder().orElse(dp.getIRI().toString())).append("\n");
            }

            response.put("success", true);
            response.put("content", sb.toString());
            response.put("baseIri", baseIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 4. Prefix / 命名空间管理 ====================

    @GetMapping("/prefixes")
    public ResponseEntity<Map<String, Object>> getPrefixes(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDocumentFormat format = manager.getOntologyFormat(ontology);

            Map<String, String> prefixes = new LinkedHashMap<>();
            if (format instanceof PrefixManager) {
                for (String pfx : ((PrefixManager) format).getPrefixNames()) {
                    prefixes.put(pfx, ((PrefixManager) format).getPrefix(pfx));
                }
            }

            response.put("success", true);
            response.put("prefixes", prefixes);
            response.put("ontologyIRI", ontology.getOntologyID().getOntologyIRI().map(IRI::toString).orElse(""));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/prefixes")
    public ResponseEntity<Map<String, Object>> updatePrefixes(
            @RequestParam String repoId,
            @RequestBody Map<String, String> prefixes) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDocumentFormat format = manager.getOntologyFormat(ontology);

            if (format instanceof PrefixManager) {
                PrefixManager pm = (PrefixManager) format;
                for (Map.Entry<String, String> e : prefixes.entrySet()) {
                    String pfx = e.getKey();
                    String ns = e.getValue();
                    if (pfx != null && ns != null) {
                        pfx = pfx.endsWith(":") ? pfx : pfx + ":";
                        pm.setPrefix(pfx, ns);
                    }
                }
                manager.saveOntology(ontology, format, new FileDocumentTarget(owlFile));
            } else {
                response.put("success", false); response.put("error", "Format does not support prefixes");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "Prefixes updated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 5. 本体导入 ====================

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importOntology(
            @RequestParam String repoId,
            @RequestParam(value = "importFile", required = false) MultipartFile importFile,
            @RequestParam(value = "importUrl", required = false) String importUrl,
            @RequestParam(value = "importIri", required = false) String importIri) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            String importedIri = null;

            if (importFile != null && !importFile.isEmpty()) {
                File tempFile = File.createTempFile("import_", ".owl");
                importFile.transferTo(tempFile);
                OWLOntology importedOnt = manager.loadOntologyFromOntologyDocument(tempFile);
                importedIri = importedOnt.getOntologyID().getOntologyIRI().map(IRI::toString).orElse("urn:imported");
                manager.applyChange(new AddImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(importedIri))));
                tempFile.delete();
                response.put("source", "file");
                response.put("classCount", importedOnt.getClassesInSignature().size());
                response.put("propertyCount", importedOnt.getObjectPropertiesInSignature().size());
            } else if (importUrl != null && !importUrl.isEmpty()) {
                manager.applyChange(new AddImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(importUrl.trim()))));
                importedIri = importUrl;
                response.put("source", "url");
            } else if (importIri != null && !importIri.isEmpty()) {
                manager.applyChange(new AddImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(importIri.trim()))));
                importedIri = importIri;
                response.put("source", "iri");
            } else {
                response.put("success", false); response.put("error", "Provide importFile, importUrl, or importIri");
                return ResponseEntity.badRequest().body(response);
            }

            saveOntology(manager, ontology, repoId);

            response.put("success", true);
            response.put("message", "Import added: " + importedIri);
            response.put("ontologyIri", importedIri);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("importOntology failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/imports")
    public ResponseEntity<Map<String, Object>> getImports(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            List<Map<String, Object>> imports = new ArrayList<>();
            for (OWLImportsDeclaration decl : ontology.getImportsDeclarations()) {
                Map<String, Object> m = new HashMap<>();
                m.put("iri", decl.getIRI().toString());
                m.put("prefix", decl.getIRI().getRemainder().orElse(""));
                imports.add(m);
            }

            List<String> closure = new ArrayList<>();
            for (OWLOntology imported : ontology.getImportsClosure()) {
                imported.getOntologyID().getOntologyIRI().ifPresent(i -> closure.add(i.toString()));
            }

            response.put("success", true);
            response.put("directImports", imports);
            response.put("importClosure", closure);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/imports")
    public ResponseEntity<Map<String, Object>> removeImport(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String importIri = (String) request.get("importIri");
            if (repoId == null || importIri == null) {
                response.put("success", false); response.put("error", "repoId and importIri required");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            for (OWLImportsDeclaration decl : ontology.getImportsDeclarations()) {
                if (decl.getIRI().toString().equals(importIri)) {
                    manager.applyChange(new RemoveImport(ontology, decl));
                }
            }

            saveOntology(manager, ontology, repoId);

            response.put("success", true);
            response.put("message", "Import removed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 6. 本体比较与合并 ====================

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareOntologies(
            @RequestParam String repoId,
            @RequestParam(required = false) String otherRepoId,
            @RequestParam(value = "otherFile", required = false) MultipartFile otherFile) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont1 = manager.loadOntologyFromOntologyDocument(owlFile);

            OWLOntology ont2 = null;
            if (otherRepoId != null && !otherRepoId.isEmpty()) {
                File otherOwlFile = getOwlFile(otherRepoId);
                ont2 = manager.loadOntologyFromOntologyDocument(otherOwlFile);
            } else if (otherFile != null && !otherFile.isEmpty()) {
                File tempFile = File.createTempFile("compare_", ".owl");
                otherFile.transferTo(tempFile);
                ont2 = manager.loadOntologyFromOntologyDocument(tempFile);
                tempFile.delete();
            } else {
                response.put("success", false); response.put("error", "Provide otherRepoId or otherFile");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> diff = new LinkedHashMap<>();

            Set<String> classes1 = new HashSet<>();
            Set<String> classes2 = new HashSet<>();
            for (OWLClass c : ont1.getClassesInSignature()) classes1.add(c.getIRI().toString());
            for (OWLClass c : ont2.getClassesInSignature()) classes2.add(c.getIRI().toString());
            diff.put("addedClasses", new ArrayList<>(difference(classes2, classes1)));
            diff.put("removedClasses", new ArrayList<>(difference(classes1, classes2)));
            diff.put("commonClasses", new ArrayList<>(intersection(classes1, classes2)));

            Set<String> ops1 = new HashSet<>();
            Set<String> ops2 = new HashSet<>();
            for (OWLObjectProperty op : ont1.getObjectPropertiesInSignature()) ops1.add(op.getIRI().toString());
            for (OWLObjectProperty op : ont2.getObjectPropertiesInSignature()) ops2.add(op.getIRI().toString());
            diff.put("addedObjectProperties", new ArrayList<>(difference(ops2, ops1)));
            diff.put("removedObjectProperties", new ArrayList<>(difference(ops1, ops2)));

            Set<String> dps1 = new HashSet<>();
            Set<String> dps2 = new HashSet<>();
            for (OWLDataProperty dp : ont1.getDataPropertiesInSignature()) dps1.add(dp.getIRI().toString());
            for (OWLDataProperty dp : ont2.getDataPropertiesInSignature()) dps2.add(dp.getIRI().toString());
            diff.put("addedDataProperties", new ArrayList<>(difference(dps2, dps1)));
            diff.put("removedDataProperties", new ArrayList<>(difference(dps1, dps2)));

            Set<String> inds1 = new HashSet<>();
            Set<String> inds2 = new HashSet<>();
            for (OWLNamedIndividual ind : ont1.getIndividualsInSignature()) inds1.add(ind.getIRI().toString());
            for (OWLNamedIndividual ind : ont2.getIndividualsInSignature()) inds2.add(ind.getIRI().toString());
            diff.put("addedIndividuals", new ArrayList<>(difference(inds2, inds1)));
            diff.put("removedIndividuals", new ArrayList<>(difference(inds1, inds2)));

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("ontology1_classes", ont1.getClassesInSignature().size());
            stats.put("ontology2_classes", ont2.getClassesInSignature().size());
            stats.put("ontology1_axioms", ont1.getAxiomCount());
            stats.put("ontology2_axioms", ont2.getAxiomCount());
            diff.put("stats", stats);

            response.put("success", true);
            response.put("diff", diff);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("compareOntologies failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> mergeOntologies(
            @RequestParam String repoId,
            @RequestParam String otherRepoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology targetOnt = manager.loadOntologyFromOntologyDocument(owlFile);

            File sourceFile = getOwlFile(otherRepoId);
            if (!sourceFile.exists()) {
                response.put("success", false); response.put("error", "Source ontology file not found: " + otherRepoId);
                return ResponseEntity.badRequest().body(response);
            }
            OWLOntology sourceOnt = manager.loadOntologyFromOntologyDocument(sourceFile);

            List<OWLOntologyChange> changes = new ArrayList<>();
            int added = 0;
            for (OWLAxiom ax : sourceOnt.getAxioms()) {
                if (!targetOnt.containsAxiom(ax)) {
                    manager.addAxiom(targetOnt, ax);
                    changes.add(new AddAxiom(targetOnt, ax));
                    added++;
                }
            }

            saveOntology(manager, targetOnt, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Merge completed");
            response.put("changesApplied", added);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("mergeOntologies failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 7. 撤销 / 重做 ====================

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        Deque<Set<OWLOntologyChange>> undo = undoStack.getOrDefault(repoId, new ArrayDeque<>());
        Deque<Set<OWLOntologyChange>> redo = redoStack.getOrDefault(repoId, new ArrayDeque<>());
        response.put("success", true);
        response.put("undoSize", undo.size());
        response.put("redoSize", redo.size());
        response.put("canUndo", !undo.isEmpty());
        response.put("canRedo", !redo.isEmpty());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/undo")
    public ResponseEntity<Map<String, Object>> undo(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Deque<Set<OWLOntologyChange>> undo = undoStack.get(repoId);
            if (undo == null || undo.isEmpty()) {
                response.put("success", false); response.put("error", "Nothing to undo");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            Set<OWLOntologyChange> lastChange = undo.pop();
            Set<OWLOntologyChange> inverted = invertChanges(lastChange);
            manager.applyChanges(new ArrayList<>(inverted));
            saveOntology(manager, ontology, repoId);

            redoStack.computeIfAbsent(repoId, k -> new ArrayDeque<>()).push(lastChange);

            response.put("success", true);
            response.put("message", "Undo successful, " + undo.size() + " operations remaining");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/redo")
    public ResponseEntity<Map<String, Object>> redo(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Deque<Set<OWLOntologyChange>> redo = redoStack.get(repoId);
            if (redo == null || redo.isEmpty()) {
                response.put("success", false); response.put("error", "Nothing to redo");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);

            Set<OWLOntologyChange> lastRedo = redo.pop();
            manager.applyChanges(new ArrayList<>(lastRedo));
            saveOntology(manager, ontology, repoId);

            undoStack.computeIfAbsent(repoId, k -> new ArrayDeque<>()).push(lastRedo);

            response.put("success", true);
            response.put("message", "Redo successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        undoStack.remove(repoId);
        redoStack.remove(repoId);
        response.put("success", true);
        response.put("message", "History cleared for repo: " + repoId);
        return ResponseEntity.ok(response);
    }

    // ==================== 8. Property Chains ====================

    @GetMapping("/propertychains")
    public ResponseEntity<Map<String, Object>> getPropertyChains(@RequestParam String repoId) {
        Map<String, Object> response = new HashMap<>();
        try {
            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            String baseIri = getBaseIri(repoId);

            List<Map<String, Object>> chains = new ArrayList<>();
            for (OWLSubPropertyChainOfAxiom ax : ontology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF)) {
                List<String> chainIris = new ArrayList<>();
                for (OWLObjectPropertyExpression expr : ax.getPropertyChain()) {
                    chainIris.add(shortLabel(expr.asOWLObjectProperty(), baseIri));
                }
                Map<String, Object> chain = new LinkedHashMap<>();
                chain.put("propertyChain", chainIris);
                chain.put("superProperty", shortLabel(ax.getSuperProperty().asOWLObjectProperty(), baseIri));
                chain.put("expression", String.join(" o ", chainIris) + " SubPropertyOf " + shortLabel(ax.getSuperProperty().asOWLObjectProperty(), baseIri));
                chains.add(chain);
            }

            response.put("success", true);
            response.put("chains", chains);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/propertychains")
    public ResponseEntity<Map<String, Object>> addPropertyChain(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            @SuppressWarnings("unchecked")
            List<String> chainPropertyIris = (List<String>) request.get("propertyChain");
            String superPropertyIri = (String) request.get("superPropertyIri");

            if (repoId == null || chainPropertyIris == null || superPropertyIri == null) {
                response.put("success", false); response.put("error", "repoId, propertyChain, and superPropertyIri required");
                return ResponseEntity.badRequest().body(response);
            }
            if (chainPropertyIris.size() < 2) {
                response.put("success", false); response.put("error", "propertyChain must contain at least 2 properties");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            List<OWLObjectPropertyExpression> chain = new ArrayList<>();
            for (String pIri : chainPropertyIris) {
                chain.add(df.getOWLObjectProperty(IRI.create(pIri.trim())));
            }
            OWLObjectProperty superProp = df.getOWLObjectProperty(IRI.create(superPropertyIri.trim()));

            OWLAxiom ax = df.getOWLSubPropertyChainOfAxiom(chain, superProp);
            manager.addAxiom(ontology, ax);
            List<OWLOntologyChange> changes = new ArrayList<>();
            changes.add(new AddAxiom(ontology, ax));

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Property chain added");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("addPropertyChain failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/propertychains")
    public ResponseEntity<Map<String, Object>> deletePropertyChain(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            @SuppressWarnings("unchecked")
            List<String> chainPropertyIris = (List<String>) request.get("propertyChain");
            String superPropertyIri = (String) request.get("superPropertyIri");

            if (repoId == null || chainPropertyIris == null || superPropertyIri == null) {
                response.put("success", false); response.put("error", "repoId, propertyChain, and superPropertyIri required");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            List<OWLObjectPropertyExpression> chain = new ArrayList<>();
            for (String pIri : chainPropertyIris) {
                chain.add(df.getOWLObjectProperty(IRI.create(pIri.trim())));
            }
            OWLSubPropertyChainOfAxiom target = df.getOWLSubPropertyChainOfAxiom(
                    chain, df.getOWLObjectProperty(IRI.create(superPropertyIri.trim())));

            manager.removeAxiom(ontology, target);
            List<OWLOntologyChange> changes = new ArrayList<>();
            changes.add(new RemoveAxiom(ontology, target));

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Property chain deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 9. 等价类 ====================

    @PostMapping("/equivalents")
    public ResponseEntity<Map<String, Object>> addEquivalentClass(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            @SuppressWarnings("unchecked")
            List<String> classIris = (List<String>) request.get("classIris");
            if (repoId == null || classIris == null || classIris.size() < 2) {
                response.put("success", false); response.put("error", "repoId and at least 2 classIris required");
                return ResponseEntity.badRequest().body(response);
            }

            File owlFile = getOwlFile(repoId);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(owlFile);
            OWLDataFactory df = manager.getOWLDataFactory();

            List<OWLClass> classes = new ArrayList<>();
            for (String iri : classIris) {
                if (iri != null && !iri.trim().isEmpty()) {
                    classes.add(df.getOWLClass(IRI.create(iri.trim())));
                }
            }
            if (classes.size() < 2) {
                response.put("success", false); response.put("error", "At least 2 valid class IRIs required");
                return ResponseEntity.badRequest().body(response);
            }

            OWLAxiom ax = df.getOWLEquivalentClassesAxiom(new LinkedHashSet<>(classes));
            manager.addAxiom(ontology, ax);
            List<OWLOntologyChange> changes = new ArrayList<>();
            changes.add(new AddAxiom(ontology, ax));

            saveOntology(manager, ontology, repoId);
            pushUndo(repoId, new HashSet<>(changes));

            response.put("success", true);
            response.put("message", "Equivalent classes added");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 10. OBDA 映射增删 ====================

    @PostMapping("/obda/mappings")
    public ResponseEntity<Map<String, Object>> addObdaMapping(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String mappingId = (String) request.get("mappingId");
            String targetQuery = (String) request.get("targetQuery");
            String sourceQuery = (String) request.get("sourceQuery");

            if (repoId == null || mappingId == null) {
                response.put("success", false); response.put("error", "repoId and mappingId required");
                return ResponseEntity.badRequest().body(response);
            }

            File obdaFile = getObdaFile(repoId);
            String currentContent = "";
            if (obdaFile.exists()) {
                currentContent = new String(Files.readAllBytes(obdaFile.toPath()), StandardCharsets.UTF_8);
                if (!currentContent.isEmpty() && !currentContent.trim().endsWith("\n")) {
                    currentContent += "\n";
                }
            }

            StringBuilder newMapping = new StringBuilder();
            newMapping.append("\n# Mapping added: ").append(java.time.LocalDateTime.now()).append("\n");
            newMapping.append("mappingId   ").append(mappingId).append("\n");
            if (targetQuery != null) newMapping.append("target ").append(targetQuery).append("\n");
            if (sourceQuery != null) newMapping.append("source ").append(sourceQuery).append("\n");
            newMapping.append("\n");

            try (FileWriter writer = new FileWriter(obdaFile)) {
                writer.write(currentContent + newMapping.toString());
            }

            response.put("success", true);
            response.put("message", "OBDA mapping added: " + mappingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("addObdaMapping failed", e);
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/obda/mappings")
    public ResponseEntity<Map<String, Object>> deleteObdaMapping(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String repoId = (String) request.get("repoId");
            String mappingId = (String) request.get("mappingId");
            if (repoId == null || mappingId == null) {
                response.put("success", false); response.put("error", "repoId and mappingId required");
                return ResponseEntity.badRequest().body(response);
            }

            File obdaFile = getObdaFile(repoId);
            if (!obdaFile.exists()) {
                response.put("success", false); response.put("error", "OBDA file not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String content = new String(Files.readAllBytes(obdaFile.toPath()), StandardCharsets.UTF_8);
            StringBuilder newContent = new StringBuilder();
            boolean skipBlock = false;
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("mappingId")) {
                    String id = trimmed.substring("mappingId".length()).trim();
                    if (id.equals(mappingId.trim())) {
                        skipBlock = true;
                        continue;
                    } else {
                        skipBlock = false;
                    }
                }
                if (skipBlock) {
                    if (trimmed.isEmpty() || trimmed.startsWith("mappingId") || trimmed.startsWith("[[")) {
                        skipBlock = false;
                        if (trimmed.startsWith("mappingId")) {
                            newContent.append(line).append("\n");
                        }
                    }
                    continue;
                }
                newContent.append(line).append("\n");
            }
            try (FileWriter writer = new FileWriter(obdaFile)) {
                writer.write(newContent.toString());
            }

            response.put("success", true);
            response.put("message", "Mapping deleted: " + mappingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false); response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 工具方法 ====================

    private <T> Set<T> difference(Set<T> a, Set<T> b) {
        Set<T> diff = new HashSet<>(a);
        diff.removeAll(b);
        return diff;
    }

    private <T> Set<T> intersection(Set<T> a, Set<T> b) {
        Set<T> inter = new HashSet<>(a);
        inter.retainAll(b);
        return inter;
    }
}
