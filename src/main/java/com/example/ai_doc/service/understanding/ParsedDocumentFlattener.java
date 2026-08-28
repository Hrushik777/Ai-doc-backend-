package com.example.ai_doc.service.understanding;

import com.example.ai_doc.model.document.ExtractedDocumentData;
import com.example.ai_doc.model.document.ExtractedField;
import com.example.ai_doc.model.layout.BBox;
import com.example.ai_doc.model.layout.DocumentElement;
import com.example.ai_doc.model.layout.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat name/value view of positioned elements, preserving the behaviour that callers of
 * {@code extractFields} already depend on: split on a leading colon where there is one,
 * otherwise fall back to the block type as the name.
 *
 * <p>Deliberately shallow. This is a compatibility view and the input to the semantic
 * fallback - it is not the pipeline route to structure. Structure comes from the layout
 * analysis, which can see the whole page instead of one element at a time.
 */
@Component
public class ParsedDocumentFlattener {

    /** Beyond this a "label" is a sentence that happens to contain a colon. */
    private static final int MAX_LABEL_LENGTH = 120;

    public ExtractedDocumentData flatten(ParsedDocument parsed) {
        List<ExtractedField> fields = new ArrayList<>(parsed.elements().size());

        for (DocumentElement element : parsed.elements()) {
            String rawText = element.textOrEmpty();
            String name = element.type();
            String value = rawText;

            int separatorIndex = rawText.indexOf(':');
            if (separatorIndex > 0) {
                String candidateName = rawText.substring(0, separatorIndex).strip();
                String candidateValue = rawText.substring(separatorIndex + 1).strip();
                if (!candidateName.isBlank()
                        && candidateName.length() <= MAX_LABEL_LENGTH
                        && !candidateName.contains("\n")
                        && !candidateValue.isBlank()) {
                    name = candidateName;
                    value = candidateValue;
                }
            }

            BBox bbox = element.bbox();
            fields.add(new ExtractedField(
                    name,
                    value,
                    element.confidence(),
                    element.page(),
                    bbox.xmin(),
                    bbox.ymin(),
                    bbox.width(),
                    bbox.height(),
                    element.type(),
                    rawText));
        }

        return new ExtractedDocumentData(fields);
    }
}
