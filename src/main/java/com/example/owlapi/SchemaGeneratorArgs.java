package com.example.owlapi;

import java.util.HashSet;
import java.util.Set;

public class SchemaGeneratorArgs {
  public String jdbcUrl;
  public String user;
  public String password;
  public String catalog;
  public String schema;
  public String tablePattern = "%";
  public Set<String> includeTables = new HashSet<>();
  public Set<String> excludeTables = new HashSet<>();
  public String baseIri = "http://example.com/auto#";
  public String output = "schema-auto.owl";
  public String driverClass = "com.mysql.cj.jdbc.Driver";

  static SchemaGeneratorArgs fromArgs(String[] args) {
    SchemaGeneratorArgs c = new SchemaGeneratorArgs();
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if ("--jdbcUrl".equals(a) && i + 1 < args.length) c.jdbcUrl = args[++i];
      else if ("--user".equals(a) && i + 1 < args.length) c.user = args[++i];
      else if ("--password".equals(a) && i + 1 < args.length) c.password = args[++i];
      else if ("--catalog".equals(a) && i + 1 < args.length) c.catalog = args[++i];
      else if ("--schema".equals(a) && i + 1 < args.length) c.schema = args[++i];
      else if ("--tablePattern".equals(a) && i + 1 < args.length) c.tablePattern = args[++i];
      else if ("--baseIri".equals(a) && i + 1 < args.length) c.baseIri = args[++i];
      else if ("--output".equals(a) && i + 1 < args.length) c.output = args[++i];
      else if ("--driverClass".equals(a) && i + 1 < args.length) c.driverClass = args[++i];
      else if ("--includeTables".equals(a) && i + 1 < args.length) c.includeTables = parseCsv(args[++i]);
      else if ("--excludeTables".equals(a) && i + 1 < args.length) c.excludeTables = parseCsv(args[++i]);
    }

    if (c.jdbcUrl == null || c.jdbcUrl.isEmpty()) {
      throw new IllegalArgumentException("Missing --jdbcUrl");
    }
    if (c.user == null) c.user = "";
    if (c.password == null) c.password = "";
    return c;
  }

  private static Set<String> parseCsv(String csv) {
    Set<String> s = new HashSet<>();
    if (csv == null || csv.trim().isEmpty()) return s;
    String[] items = csv.split(",");
    for (String it : items) {
      String v = it.trim();
      if (!v.isEmpty()) s.add(v.toLowerCase());
    }
    return s;
  }
}

