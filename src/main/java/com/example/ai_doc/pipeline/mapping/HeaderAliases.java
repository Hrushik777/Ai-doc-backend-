package com.example.ai_doc.pipeline.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves an abbreviation to the canonical name it stands for, so that a document field
 * and a template header meaning the same thing collapse to the same key.
 *
 * <p>Canonicalization has to be applied to <em>both</em> sides. Applying it only to the
 * document field - as this pipeline used to - makes matching one-directional: a field named
 * {@code Mfr} finds a header named {@code Manufacturer}, but a header spelled {@code Mfr}
 * matches nothing at all, because the header index was keyed on its raw spelling. Running
 * both through here means all four combinations resolve.
 *
 * <p>The default entries are engineering terms that suited the first documents this
 * pipeline saw. They are not meant to be a permanent part of a generic tool, so
 * {@code app.mapping.aliases} replaces them wholesale for a deployment that needs a
 * different vocabulary - or none.
 */
@Component
public class HeaderAliases {

    private static final Map<String, String> DEFAULT_ALIASES = Map.of(
            "mfr", "manufacturer",
            "mawp", "maximum allowable working pressure");

    /** Set {@code app.mapping.aliases} to this to run with no aliases at all. */
    public static final String NONE = "none";

    private final Map<String, String> canonicalByAlias;

    public HeaderAliases() {
        this("");
    }

    /**
     * @param specification {@code alias=canonical} pairs separated by semicolons. Blank
     *                      keeps the defaults; {@value #NONE} disables aliasing entirely.
     */
    @Autowired
    public HeaderAliases(@Value("${app.mapping.aliases:}") String specification) {
        this.canonicalByAlias = parse(specification);
    }

    private Map<String, String> parse(String specification) {
        if (specification == null || specification.isBlank()) {
            return DEFAULT_ALIASES;
        }
        if (NONE.equalsIgnoreCase(specification.strip())) {
            return Map.of();
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        for (String entry : specification.split(";")) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String alias = pair[0].strip().toLowerCase(Locale.ROOT);
            String canonical = pair[1].strip().toLowerCase(Locale.ROOT);
            if (!alias.isEmpty() && !canonical.isEmpty()) {
                parsed.put(alias, canonical);
            }
        }
        return Map.copyOf(parsed);
    }

    /**
     * @param normalizedName a name already put through {@link HeaderNameNormalizer}
     * @return the canonical form, or the name unchanged when it is not an alias
     */
    public String canonicalize(String normalizedName) {
        return canonicalByAlias.getOrDefault(normalizedName, normalizedName);
    }
}
