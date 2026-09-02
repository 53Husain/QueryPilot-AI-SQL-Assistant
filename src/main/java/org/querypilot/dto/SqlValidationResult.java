package org.querypilot.dto;

public class SqlValidationResult {

    private boolean valid;
    private String message;
    private String normalizedSql;

    public SqlValidationResult() {
    }

    public SqlValidationResult(
            boolean valid,
            String message,
            String normalizedSql) {

        this.valid = valid;
        this.message = message;
        this.normalizedSql = normalizedSql;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNormalizedSql() {
        return normalizedSql;
    }

    public void setNormalizedSql(String normalizedSql) {
        this.normalizedSql = normalizedSql;
    }

    public String getSql() {
        return normalizedSql;
    }

    public void setSql(String sql) {
        this.normalizedSql = sql;
    }
}