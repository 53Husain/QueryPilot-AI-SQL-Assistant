package org.querypilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QueryRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 500, message = "Question cannot exceed 500 characters")
    private String question;

    public QueryRequest() {
    }

    public QueryRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}