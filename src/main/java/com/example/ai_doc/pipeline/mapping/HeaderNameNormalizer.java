package com.example.ai_doc.pipeline.mapping;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalizes labels for deterministic, case-insensitive exact matching. */
@Component
public class HeaderNameNormalizer {

    // String.replaceAll(..) recompiles this pattern on every call, and normalize() runs
    // once per header per extracted field. Compiling once turns the dominant cost of the
    // deterministic stage into a plain scan.
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    public String normalize(String value) {
        if (value == null) {
            return "";
        }

        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return "";
        }

        // Only pay for the regex when the label actually contains a whitespace run to
        // collapse; the common case (single-word or already-single-spaced) skips it.
        if (needsWhitespaceCollapsing(stripped)) {
            stripped = WHITESPACE_RUN.matcher(stripped).replaceAll(" ");
        }

        return stripped.toLowerCase(Locale.ROOT);
    }

    private boolean needsWhitespaceCollapsing(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isWhitespace(character)
                    && (character != ' ' || (i + 1 < value.length() && Character.isWhitespace(value.charAt(i + 1))))) {
                return true;
            }
        }
        return false;
    }
}
