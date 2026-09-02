package org.querypilot.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DatabaseSchemaService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getDatabaseSchema() {

        String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        StringBuilder schema = new StringBuilder();

        String tableQuery = """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = ?
                AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """;

        List<String> tables = jdbcTemplate.queryForList(
                tableQuery,
                String.class,
                databaseName
        );

        for (String table : tables) {

            schema.append("Table: ")
                    .append(table)
                    .append("\n");

            String columnQuery = """
                    SELECT COLUMN_NAME, DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = ?
                    AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """;

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    columnQuery,
                    databaseName,
                    table
            );

            schema.append("Columns:\n");

            for (Map<String, Object> column : columns) {

                schema.append(column.get("COLUMN_NAME"))
                        .append(" ")
                        .append(column.get("DATA_TYPE"))
                        .append("\n");
            }

            schema.append("\n");
        }

        return schema.toString().trim();
    }
}