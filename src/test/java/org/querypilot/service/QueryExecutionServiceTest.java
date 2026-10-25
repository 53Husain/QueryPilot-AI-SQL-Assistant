package org.querypilot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryExecutionServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final QueryExecutionService service = new QueryExecutionService(jdbc);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "DELETE FROM employees", "UPDATE employees SET salary = 0",
            "SELECT * FROM employees; SELECT * FROM secrets"})
    void rejectedStatementsNeverReachJdbc(String sql) {
        assertThrows(IllegalArgumentException.class, () -> service.executeQuery(sql));
        verifyNoInteractions(jdbc);
    }

    @Test
    void returnsDatabaseRowsForReadQuery() {
        String sql = "SELECT name FROM employees;";
        List<Map<String, Object>> rows = List.of(Map.of("name", "Asha"));
        when(jdbc.queryForList(sql)).thenReturn(rows);
        assertEquals(rows, service.executeQuery(sql));
        verify(jdbc).queryForList(sql);
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void databaseFailureIsNotReportedAsEmptySuccess() {
        String sql = "SELECT name FROM employees";
        when(jdbc.queryForList(sql)).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThrows(DataAccessResourceFailureException.class, () -> service.executeQuery(sql));
    }
}
