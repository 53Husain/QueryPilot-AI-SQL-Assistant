package org.querypilot.service;

import org.querypilot.dto.QueryRequest;
import org.querypilot.dto.QueryResponse;
import org.querypilot.dto.SqlValidationResult;
import org.querypilot.llm.LlmService;
import org.querypilot.validator.SqlValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final DatabaseSchemaService databaseSchemaService;
    private final LlmService llmService;
    private final SqlValidator sqlValidator;
    private final QueryExecutionService queryExecutionService;

    public QueryService(
            DatabaseSchemaService databaseSchemaService,
            LlmService llmService,
            SqlValidator sqlValidator,
            QueryExecutionService queryExecutionService) {

        this.databaseSchemaService = databaseSchemaService;
        this.llmService = llmService;
        this.sqlValidator = sqlValidator;
        this.queryExecutionService = queryExecutionService;
    }

    public QueryResponse generateQuery(QueryRequest request) {

        long startTime = System.currentTimeMillis();

        String question = request.getQuestion();

        String databaseSchema =
                databaseSchemaService.getDatabaseSchema();

        String generatedSql =
                llmService.generateSql(
                        question,
                        databaseSchema
                );

        // AI could not generate a query for the question
        if ("INVALID_QUERY".equalsIgnoreCase(generatedSql.trim())) {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            return new QueryResponse(
                    question,
                    null,
                    List.of(),
                    0,
                    executionTime,
                    "Question cannot be answered using the available database schema."
            );
        }

        // Validate AI-generated SQL before execution
        SqlValidationResult validationResult =
                sqlValidator.validate(generatedSql);

        if (!validationResult.isValid()) {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            return new QueryResponse(
                    question,
                    validationResult.getNormalizedSql(),
                    List.of(),
                    0,
                    executionTime,
                    validationResult.getMessage()
            );
        }

        String validatedSql =
                validationResult.getNormalizedSql();

        // Execute validated SELECT query
        List<Map<String, Object>> results =
                queryExecutionService.executeQuery(validatedSql);

        long executionTime =
                System.currentTimeMillis() - startTime;

        return new QueryResponse(
                question,
                validatedSql,
                results,
                results.size(),
                executionTime,
                "Query executed successfully"
        );
    }
}