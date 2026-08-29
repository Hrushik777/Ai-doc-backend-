package com.example.ai_doc.pipeline.mapping;

import com.example.ai_doc.domain.excel.ExcelColumn;
import com.example.ai_doc.domain.mapping.IndexedExtractedField;
import com.example.ai_doc.domain.mapping.SemanticMapping;

import java.util.List;

/** Semantic fallback used only for fields that have no normalized exact header match. */
public interface SemanticMappingService {

    List<SemanticMapping> mapUnmatchedFields(List<IndexedExtractedField> unmatchedFields,
                                             List<ExcelColumn> headers);
}
