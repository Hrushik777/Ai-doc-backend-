package com.example.ai_doc.model.mapping;

import java.util.List;

/** Expected JSON response shape from the semantic mapping LLM. */
public record SemanticMappingResponse(List<SemanticMapping> mappings) {

    public SemanticMappingResponse {
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }
}
