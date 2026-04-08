package com.example.owlapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ontology")
public class SystemBuiltinProperties {

  private GraphDb graphDb = new GraphDb();

  public GraphDb getGraphDb() {
    return graphDb;
  }

  public void setGraphDb(GraphDb graphDb) {
    this.graphDb = graphDb;
  }

  public static class GraphDb {
    private String url = "http://localhost:7200";
    private String repositoryId = "medical";
    private String username = "";
    private String password = "";

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

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}

