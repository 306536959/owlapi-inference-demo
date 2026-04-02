package com.example.owlapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "ontology")
public class SystemBuiltinProperties {

  private Bootstrap bootstrap = new Bootstrap();
  private GraphDb graphDb = new GraphDb();

  public Bootstrap getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  public GraphDb getGraphDb() {
    return graphDb;
  }

  public void setGraphDb(GraphDb graphDb) {
    this.graphDb = graphDb;
  }

  public static class Bootstrap {
    private boolean enabled = false;
    private String jdbcUrl;
    private String user = "";
    private String password = "";
    private String catalog;
    private String schema;
    private String tablePattern = "%";
    private List<String> includeTables = new ArrayList<>();
    private List<String> excludeTables = new ArrayList<>();
    private String baseIri = "http://example.com/auto#";
    private String output = "schema-auto.owl";
    private String obdaOutput = "schema-auto.obda";
    private String driverClass = "com.mysql.cj.jdbc.Driver";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getJdbcUrl() {
      return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getCatalog() {
      return catalog;
    }

    public void setCatalog(String catalog) {
      this.catalog = catalog;
    }

    public String getSchema() {
      return schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
    }

    public String getTablePattern() {
      return tablePattern;
    }

    public void setTablePattern(String tablePattern) {
      this.tablePattern = tablePattern;
    }

    public List<String> getIncludeTables() {
      return includeTables;
    }

    public void setIncludeTables(List<String> includeTables) {
      this.includeTables = includeTables;
    }

    public List<String> getExcludeTables() {
      return excludeTables;
    }

    public void setExcludeTables(List<String> excludeTables) {
      this.excludeTables = excludeTables;
    }

    public String getBaseIri() {
      return baseIri;
    }

    public void setBaseIri(String baseIri) {
      this.baseIri = baseIri;
    }

    public String getOutput() {
      return output;
    }

    public void setOutput(String output) {
      this.output = output;
    }

    public String getDriverClass() {
      return driverClass;
    }

    public void setDriverClass(String driverClass) {
      this.driverClass = driverClass;
    }

    public String getObdaOutput() {
      return obdaOutput;
    }

    public void setObdaOutput(String obdaOutput) {
      this.obdaOutput = obdaOutput;
    }
  }

  public static class GraphDb {
    private String url = "http://localhost:7200";
    private String repositoryId = "medical";

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getRepositoryId() {
      return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
      this.repositoryId = repositoryId;
    }
  }
}

