package org.querypilot.controller;

import jakarta.validation.Valid;
import org.querypilot.dto.QueryRequest;
import org.querypilot.dto.QueryResponse;
import org.querypilot.service.QueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@CrossOrigin
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public QueryResponse generateQuery(
            @Valid @RequestBody QueryRequest request) {

        return queryService.generateQuery(request);
    }
}