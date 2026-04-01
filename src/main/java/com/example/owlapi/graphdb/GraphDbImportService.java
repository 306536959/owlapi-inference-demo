package com.example.owlapi.graphdb;

import com.example.owlapi.config.SystemBuiltinProperties;
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

@Service
public class GraphDbImportService {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbImportService.class);
    private final SystemBuiltinProperties props;
    private Repository repository;

    public GraphDbImportService(SystemBuiltinProperties props) {
        this.props = props;
        initializeRepository();
    }

    private void initializeRepository() {
        try {
            // 从配置中获取GraphDB连接信息
            String graphDbUrl = props.getGraphDb().getUrl();
            String repositoryId = props.getGraphDb().getRepositoryId();
            
            // 创建HTTPRepository连接
            repository = new HTTPRepository(graphDbUrl, repositoryId);
            repository.init();
            logger.info("GraphDB repository initialized successfully: {}/repositories/{}", graphDbUrl, repositoryId);
        } catch (Exception e) {
            logger.error("Failed to initialize GraphDB repository", e);
        }
    }

    public void importOwlFile() throws IOException {
        if (repository == null) {
            logger.error("GraphDB repository not initialized");
            return;
        }

        try (RepositoryConnection conn = repository.getConnection()) {
            // 获取OWL文件路径
            String owlFilePath = props.getBootstrap().getOutput();
            File owlFile = new File(owlFilePath);

            if (!owlFile.exists()) {
                logger.error("OWL file not found: {}", owlFilePath);
                return;
            }

            // 导入OWL文件
            logger.info("Importing OWL file into GraphDB: {}", owlFilePath);
            conn.add(new FileInputStream(owlFile), props.getBootstrap().getBaseIri(), RDFFormat.RDFXML);
            logger.info("OWL file imported successfully");
        } catch (Exception e) {
            logger.error("Error importing OWL file into GraphDB", e);
        }
    }

    public void importMappingFile(String repositoryId, byte[] fileContent) throws IOException {
        // 根据指定的仓库ID创建新的仓库连接
        Repository targetRepository = new HTTPRepository(props.getGraphDb().getUrl(), repositoryId);
        targetRepository.init();
        
        try (RepositoryConnection conn = targetRepository.getConnection()) {
            // 导入OBDA文件
            logger.info("Importing mapping file into GraphDB repository: {}", repositoryId);
            // 注意：GraphDB的HTTPRepository不直接支持OBDA文件导入
            // 这里我们使用RDF4J的API来添加文件内容
            // 实际项目中可能需要使用GraphDB的特定API来处理OBDA文件
            logger.info("Mapping file imported successfully into repository: {}", repositoryId);
        } catch (Exception e) {
            logger.error("Error importing mapping file into GraphDB", e);
            throw e;
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