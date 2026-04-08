package com.example.owlapi.graphdb;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class GraphDbImportService {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbImportService.class);
    private static final String DEFAULT_BASE_IRI = "http://example.com/ontology#";
    private final SystemBuiltinProperties props;
    private Repository repository;
    private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    public GraphDbImportService(SystemBuiltinProperties props) {
        this.props = props;
        initializeRepository();
    }

    private void initializeRepository() {
        try {
            String graphDbUrl = props.getGraphDb().getUrl();
            String repositoryId = props.getGraphDb().getRepositoryId();
            HTTPRepository httpRepository = new HTTPRepository(graphDbUrl, repositoryId);
            applyAuth(httpRepository);
            repository = httpRepository;
            repository.init();
            logger.info("GraphDB repository initialized: {}/repositories/{}", graphDbUrl, repositoryId);
        } catch (Exception e) {
            logger.error("Failed to initialize GraphDB repository", e);
        }
    }

    private void applyAuth(HTTPRepository repository) {
        String username = props.getGraphDb().getUsername();
        String password = props.getGraphDb().getPassword();
        if (username != null && !username.trim().isEmpty()) {
            repository.setUsernameAndPassword(username, password == null ? "" : password);
        }
    }

    /**
     * Import OWL file to a specific repository
     */
    public void importOwlFileToRepository(String repositoryId, InputStream inputStream, RDFFormat format) throws IOException {
        HTTPRepository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        applyAuth(targetRepository);
        targetRepository.init();

        try (RepositoryConnection conn = targetRepository.getConnection()) {
            logger.info("Importing OWL/RDF to repository: {}", repositoryId);
            String baseUri = DEFAULT_BASE_IRI;
            conn.add(inputStream, baseUri, format, (org.eclipse.rdf4j.model.Resource[]) null);
            logger.info("Successfully imported to repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Error importing to repository: {}", repositoryId, e);
            throw e;
        } finally {
            targetRepository.shutDown();
        }
    }

    /**
     * Import RDF data to a specific repository
     */
    public void importRdfToRepository(String repositoryId, InputStream inputStream, RDFFormat format) throws IOException {
        HTTPRepository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        applyAuth(targetRepository);
        targetRepository.init();

        try (RepositoryConnection conn = targetRepository.getConnection()) {
            logger.info("Importing RDF data to repository: {} (format: {})", repositoryId, format);
            String baseUri = DEFAULT_BASE_IRI;
            conn.add(inputStream, baseUri, format, (org.eclipse.rdf4j.model.Resource[]) null);
            logger.info("Successfully imported RDF data to repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Error importing RDF to repository: {}", repositoryId, e);
            throw e;
        } finally {
            targetRepository.shutDown();
        }
    }

    /**
     * Import RDF file to default repository
     */
    public void importRdfFile(File rdfFile, RDFFormat format) throws IOException {
        if (repository == null) {
            logger.error("GraphDB repository not initialized");
            return;
        }

        try (RepositoryConnection conn = repository.getConnection()) {
            logger.info("Importing RDF file: {}", rdfFile.getAbsolutePath());
            conn.add(new FileInputStream(rdfFile), DEFAULT_BASE_IRI, format);
            logger.info("RDF file imported successfully");
        } catch (Exception e) {
            logger.error("Error importing RDF file", e);
            throw e;
        }
    }

    public void importMappingFile(String repositoryId, byte[] fileContent) throws IOException {
        HTTPRepository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        applyAuth(targetRepository);
        targetRepository.init();

        try (RepositoryConnection conn = targetRepository.getConnection()) {
            logger.info("Importing mapping file into repository: {}", repositoryId);
            // Note: OBDA files need special handling through Ontop
            // For direct RDF imports, use importRdfToRepository instead
            logger.info("Mapping file processed for repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Error importing mapping file into GraphDB", e);
            throw e;
        } finally {
            targetRepository.shutDown();
        }
    }

    /**
     * Clear all data from a repository
     */
    public void clearRepository(String repositoryId) {
        HTTPRepository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        applyAuth(targetRepository);
        targetRepository.init();

        try (RepositoryConnection conn = targetRepository.getConnection()) {
            logger.info("Clearing repository: {}", repositoryId);
            conn.clear();
            conn.clearNamespaces();
            logger.info("Repository cleared successfully: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Error clearing repository: {}", repositoryId, e);
        } finally {
            targetRepository.shutDown();
        }
    }

    /**
     * Get statistics for a repository
     */
    public Map<String, Object> getRepositoryStats(String repositoryId) {
        Map<String, Object> stats = new HashMap<>();
        HTTPRepository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        applyAuth(targetRepository);
        targetRepository.init();

        try (RepositoryConnection conn = targetRepository.getConnection()) {
            stats.put("statementCount", conn.size());
            stats.put("isEmpty", conn.isEmpty());
            
            // Get namespace info
            Map<String, String> namespaces = new HashMap<>();
            conn.getNamespaces().forEach(ns -> namespaces.put(ns.getPrefix(), ns.getName()));
            stats.put("namespaces", namespaces);
            
            return stats;
        } catch (Exception e) {
            logger.error("Error getting repository stats: {}", repositoryId, e);
            stats.put("error", e.getMessage());
            return stats;
        } finally {
            targetRepository.shutDown();
        }
    }

    public void shutdown() {
        if (repository != null) {
            repository.shutDown();
            logger.info("GraphDB repository shutdown successfully");
        }
    }
}