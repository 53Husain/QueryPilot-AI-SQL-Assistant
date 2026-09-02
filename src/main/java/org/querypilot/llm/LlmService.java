package org.querypilot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    public LlmService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public String generateSql(String question, String databaseSchema) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be empty"
            );
        }

        if (databaseSchema == null || databaseSchema.isBlank()) {
            throw new IllegalStateException(
                    "Database schema is unavailable"
            );
        }

        String systemPrompt = """
                You are QueryPilot, an AI SQL assistant.

                Your task is to convert a user's natural-language question
                into a valid SQL SELECT query.

                Follow these rules strictly:

                1. Generate only SELECT statements.
                2. Do not generate INSERT, UPDATE, DELETE, DROP, ALTER,
                   TRUNCATE, CREATE, GRANT, REVOKE, or other modifying statements.
                3. Use only tables and columns present in the provided schema.
                4. Do not invent tables or columns.
                5. Return only the SQL query.
                6. Do not wrap the SQL in Markdown code fences.
                7. Do not include explanations before or after the SQL.
                8. If the question cannot be answered using the provided schema,
                   return exactly: INVALID_QUERY

                Database schema:

                """ + databaseSchema;

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("model", model);

        requestBody.put(
                "messages",
                List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                systemPrompt
                        ),
                        Map.of(
                                "role",
                                "user",
                                "content",
                                question
                        )
                )
        );

        try {

            String response = restClient
                    .post()
                    .uri(apiUrl)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractSql(response);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to generate SQL using the AI service",
                    exception
            );
        }
    }

    private String extractSql(String response) {

        try {

            if (response == null || response.isBlank()) {
                throw new IllegalStateException(
                        "AI service returned an empty response"
                );
            }

            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException(
                        "AI response did not contain generated SQL"
                );
            }

            String sql = content.asText().trim();

            if (sql.startsWith("```sql")) {
                sql = sql.substring(6).trim();
            } else if (sql.startsWith("```")) {
                sql = sql.substring(3).trim();
            }

            if (sql.endsWith("```")) {
                sql = sql.substring(
                        0,
                        sql.length() - 3
                ).trim();
            }

            return sql;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to parse SQL from AI response",
                    exception
            );
        }
    }
}