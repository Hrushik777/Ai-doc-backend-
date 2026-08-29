package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.layout.DocumentLayout;

import java.util.List;

/**
 * Decides the spreadsheet columns for a document that arrived without a template.
 *
 * <p>Separate from {@link SemanticMappingService} because it answers a different question.
 * Semantic mapping is given a set of columns and asked which one a value belongs in; this
 * is asked what the columns should be in the first place.
 */
public interface HeaderInferenceService {

    /**
     * @return the header names to build a workbook around, in column order; empty when the
     *         document offered nothing to name columns after.
     */
    List<String> inferHeaders(DocumentLayout layout);
}
