package com.example.ai_doc.service.mapping;

import org.springframework.stereotype.Component;

import java.util.Locale;

/** Normalizes labels for deterministic, case-insensitive exact matching. */
@Component
public class HeaderNameNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
