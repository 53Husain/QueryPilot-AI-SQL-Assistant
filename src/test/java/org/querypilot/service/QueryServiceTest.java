package org.querypilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.querypilot.dto.QueryRequest;
import org.querypilot.llm.LlmService;
import org.querypilot.validator.SqlValidator;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryServiceTest {
    private DatabaseSchemaService schema;
    private LlmService llm;
    private QueryExecutionService execution;
    private QueryService service;
    private static final String QUESTION = "List employees";
    private static final String SCHEMA = "Table: employees\nColumns:\nid int\nname varchar";

    @BeforeEach
    void setup() {
        schema = mock(DatabaseSchemaService.class);
        llm = mock(LlmService.class);
        execution = mock(QueryExecutionService.class);
        service = new QueryService(schema, llm, new SqlValidator(), execution);
    }

    private void generated(String sql) {
        when(schema.getDatabaseSchema()).thenReturn(SCHEMA);
        when(llm.generateSql(QUESTION, SCHEMA)).thenReturn(sql);
    }

    @Test
    void passesDiscoveredSchemaAndExecutesNormalizedSql() {
        generated("```sql\nSELECT name FROM employees;\n```");
        List<Map<String, Object>> rows = List.of(Map.of("name", "Asha"));
        when(execution.executeQuery("SELECT name FROM employees")).thenReturn(rows);
        var result = service.generateQuery(new QueryRequest(QUESTION));
        assertEquals(QUESTION, result.getQuestion());
        assertEquals("SELECT name FROM employees", result.getSql());
        assertEquals(rows, result.getData());
        assertEquals(1, result.getRowCount());
        assertEquals("Query executed successfully", result.getMessage());
        verify(llm).generateSql(QUESTION, SCHEMA);
        verify(execution).executeQuery("SELECT name FROM employees");
        verifyNoMoreInteractions(execution);
    }

    @Test
    void unsupportedQuestionNeverReachesExecution() {
        generated(" INVALID_QUERY ");
        var result = service.generateQuery(new QueryRequest(QUESTION));
        assertNull(result.getSql());
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getRowCount());
        assertTrue(result.getMessage().contains("cannot be answered"));
        verifyNoInteractions(execution);
    }

    @Test
    void rejectedSqlNeverReachesExecution() {
        generated("DELETE FROM employees");
        var result = service.generateQuery(new QueryRequest(QUESTION));
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getRowCount());
        assertTrue(result.getMessage().contains("Only SELECT"));
        verifyNoInteractions(execution);
    }

    @Test
    void emptyResultsRemainSuccessful() {
        generated("SELECT name FROM employees");
        when(execution.executeQuery("SELECT name FROM employees")).thenReturn(List.of());
        var result = service.generateQuery(new QueryRequest(QUESTION));
        assertEquals(0, result.getRowCount());
        assertTrue(result.getData().isEmpty());
        assertEquals("Query executed successfully", result.getMessage());
    }

    @Test
    void schemaFailureStopsBeforeAiAndExecution() {
        when(schema.getDatabaseSchema()).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.generateQuery(new QueryRequest(QUESTION)));
        verifyNoInteractions(llm, execution);
    }

    @Test
    void aiFailureStopsBeforeExecution() {
        when(schema.getDatabaseSchema()).thenReturn(SCHEMA);
        when(llm.generateSql(QUESTION, SCHEMA)).thenThrow(new IllegalStateException("provider unavailable"));
        assertThrows(IllegalStateException.class, () -> service.generateQuery(new QueryRequest(QUESTION)));
        verifyNoInteractions(execution);
    }
}
