package com.example.owlapi;

import java.util.HashSet;
import java.util.Set;

/**
 * Arguments for SchemaToObdaGenerator.
 */
public class ObdaGeneratorArgs {

    public String jdbcUrl;
    public String user;
    public String password;
    public String catalog;
    public String schema;
    public String tablePattern = "%";
    public Set<String> includeTables = new HashSet<>();
    public Set<String> excludeTables = new HashSet<>();
    public String baseIri = "http://example.com/auto#";
    public String output = "schema-auto";
    public String driverClass = "com.mysql.cj.jdbc.Driver";

    public static ObdaGeneratorArgs fromArgs(String[] args) {
        ObdaGeneratorArgs cfg = new ObdaGeneratorArgs();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--jdbcUrl":
                    cfg.jdbcUrl = args[++i];
                    break;
                case "--user":
                    cfg.user = args[++i];
                    break;
                case "--password":
                    cfg.password = args[++i];
                    break;
                case "--catalog":
                    cfg.catalog = args[++i];
                    break;
                case "--schema":
                    cfg.schema = args[++i];
                    break;
                case "--tablePattern":
                    cfg.tablePattern = args[++i];
                    break;
                case "--includeTables":
                    String[] includes = args[++i].split(",");
                    for (String t : includes) {
                        cfg.includeTables.add(t.trim().toLowerCase());
                    }
                    break;
                case "--excludeTables":
                    String[] excludes = args[++i].split(",");
                    for (String t : excludes) {
                        cfg.excludeTables.add(t.trim().toLowerCase());
                    }
                    break;
                case "--baseIri":
                    cfg.baseIri = args[++i];
                    break;
                case "--output":
                    cfg.output = args[++i];
                    break;
                case "--driverClass":
                    cfg.driverClass = args[++i];
                    break;
            }
        }

        return cfg;
    }
}
