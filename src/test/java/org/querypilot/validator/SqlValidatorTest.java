package org.querypilot.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SqlValidatorTest {
    private final SqlValidator validator = new SqlValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t", "```sql\n```"})
    void rejectsEmptySql(String sql) {
        assertFalse(validator.validate(sql).isValid());
    }

    @Test
    void normalizesFencedSelectAndTrailingSemicolon() {
        var result = validator.validate("```sql\nSELECT name\nFROM employees;\n```");
        assertTrue(result.isValid());
        assertEquals("SELECT name FROM employees", result.getNormalizedSql());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DELETE FROM employees", "UPDATE employees SET salary = 0",
        "DROP TABLE employees", "INSERT INTO employees VALUES (1)",
        "SELECT * FROM employees; SELECT * FROM secrets",
        "SELECT * FROM employees; DROP TABLE employees;"
    })
    void rejectsWritesAndMultipleStatements(String sql) {
        assertFalse(validator.validate(sql).isValid());
    }

    @Test
    void acceptsSimpleMixedCaseRead() {
        assertTrue(validator.validate("select name FROM employees WHERE id = 1").isValid());
    }
}
