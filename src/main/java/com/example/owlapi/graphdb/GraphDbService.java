package com.example.owlapi.graphdb;

import com.example.owlapi.config.SystemBuiltinProperties;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class GraphDbService {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbService.class);
    private final SystemBuiltinProperties props;
    private Repository repository;

    public GraphDbService(SystemBuiltinProperties props) {
        this.props = props;
        initializeRepository();
    }

    private void initializeRepository() {
        try {
            // 从配置中获取GraphDB连接信息
            String graphDbUrl = props.getGraphDb().getUrl();
            String repositoryId = props.getGraphDb().getRepositoryId();
            
            // 创建HTTPRepository连接
            HTTPRepository httpRepository = new HTTPRepository(graphDbUrl, repositoryId);
            String username = props.getGraphDb().getUsername();
            String password = props.getGraphDb().getPassword();
            if (username != null && !username.trim().isEmpty()) {
                httpRepository.setUsernameAndPassword(username, password == null ? "" : password);
            }
            repository = httpRepository;
            repository.init();
            logger.info("GraphDB repository initialized successfully: {}/repositories/{}", graphDbUrl, repositoryId);
        } catch (Exception e) {
            logger.error("Failed to initialize GraphDB repository", e);
        }
    }

    public String executeSparqlQuery(String query) throws IOException {
        logger.debug("Executing SPARQL query via GraphDB: {}", query);
        
        if (repository == null) {
            logger.error("GraphDB repository not initialized");
            return "{\"head\":{\"vars\":[]},\"results\":{\"bindings\":[]}}";
        }

        try (RepositoryConnection conn = repository.getConnection()) {
            // 执行SPARQL查询
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
            
            // 处理查询结果
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                // 构建JSON响应
                StringBuilder json = new StringBuilder();
                json.append("{\"head\":{\"vars\":[");
                
                // 添加变量名
                List<String> bindingNames = result.getBindingNames();
                for (int i = 0; i < bindingNames.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(bindingNames.get(i)).append("\"");
                }
                json.append("]},\"results\":{\"bindings\":[");
                
                // 添加结果
                boolean first = true;
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    if (!first) json.append(",");
                    json.append("{");
                    
                    boolean firstBinding = true;
                    for (String var : bindingNames) {
                        Value value = bindingSet.getValue(var);
                        if (value != null) {
                            if (!firstBinding) json.append(",");
                            json.append("\"").append(var).append("\":{\"type\":\"literal\",\"value\":\"").append(value.stringValue()).append("\"");
                            firstBinding = false;
                        }
                    }
                    json.append("}");
                    first = false;
                }
                
                json.append("]}}");
                return json.toString();
            }
        } catch (Exception e) {
            logger.error("Error executing SPARQL query on GraphDB", e);
            return "{\"head\":{\"vars\":[]},\"results\":{\"bindings\":[]}}";
        }
    }

    public void shutdown() {
        if (repository != null) {
            repository.shutDown();
            logger.info("GraphDB repository shutdown successfully");
        }
    }
}