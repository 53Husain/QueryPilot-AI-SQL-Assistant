package org.querypilot.dto;

import java.util.List;
import java.util.Map;

public class QueryResponse {

    private String question;
    private String sql;
    private List<Map<String, Object>> data;
    private int rowCount;
    private long executionTimeMs;
    private String message;

    public QueryResponse() {
    }

    public QueryResponse(
            String question,
            String sql,
            List<Map<String, Object>> data,
            int rowCount,
            long executionTimeMs,
            String message) {

        this.question = question;
        this.sql = sql;
        this.data = data;
        this.rowCount = rowCount;
        this.executionTimeMs = executionTimeMs;
        this.message = message;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}