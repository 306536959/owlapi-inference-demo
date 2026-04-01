package com.example.owlapi.bootstrap;

import com.example.owlapi.config.SystemBuiltinProperties;
import com.example.owlapi.graphdb.GraphDbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GraphDbBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GraphDbBootstrapRunner.class);
    private final SystemBuiltinProperties props;
    private final GraphDbImportService graphDbImportService;

    public GraphDbBootstrapRunner(SystemBuiltinProperties props, GraphDbImportService graphDbImportService) {
        this.props = props;
        this.graphDbImportService = graphDbImportService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 检查是否启用了引导功能
        if (props.getBootstrap().isEnabled()) {
            logger.info("Starting GraphDB bootstrap process...");
            try {
                // 导入OWL文件到GraphDB
                graphDbImportService.importOwlFile();
                logger.info("GraphDB bootstrap process completed successfully");
            } catch (IOException e) {
                logger.error("Error during GraphDB bootstrap process", e);
            }
        } else {
            logger.info("GraphDB bootstrap is disabled. Skipping...");
        }
    }
}