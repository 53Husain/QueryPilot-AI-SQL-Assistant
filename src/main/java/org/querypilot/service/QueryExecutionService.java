package org.querypilot.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }

        String normalizedSql = sql
                .trim()
                .toLowerCase();

        // Remove a trailing semicolon for validation
        if (normalizedSql.endsWith(";")) {
            normalizedSql = normalizedSql.substring(
                    0,
                    normalizedSql.length() - 1
            ).trim();
        }

        // Only SELECT queries are allowed
        if (!normalizedSql.startsWith("select")) {
            throw new IllegalArgumentException(
                    "Only SELECT queries are allowed"
            );
        }

        // Prevent multiple SQL statements
        if (normalizedSql.contains(";")) {
            throw new IllegalArgumentException(
                    "Multiple SQL statements are not allowed"
            );
        }

        // Block SQL modification commands
        String[] forbiddenKeywords = {
                "insert",
                "update",
                "delete",
                "drop",
                "alter",
                "truncate",
                "create",
                "grant",
                "revoke"
        };

        for (String keyword : forbiddenKeywords) {

            if (normalizedSql.matches(
                    ".*\\b" + keyword + "\\b.*"
            )) {

                throw new IllegalArgumentException(
                        "SQL statement contains a forbidden operation: "
                                + keyword
                );
            }
        }

        return jdbcTemplate.queryForList(sql);
    }
}