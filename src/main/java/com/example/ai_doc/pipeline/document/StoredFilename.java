package com.example.ai_doc.pipeline.document;

import java.util.Locale;
import java.util.UUID;

/**
 * Turns a client-supplied filename into one that is safe to write to disk.
 *
 * <p>An uploaded filename is attacker-controlled input. Resolving it against the upload
 * directory unchanged - which is what this pipeline used to do - lets a name like
 * {@code ../../config/application.properties} write outside that directory entirely. Every
 * directory separator and every parent reference is therefore discarded here rather than
 * being validated away, because rejecting known-bad shapes leaves the ones nobody thought
 * of, while keeping only the final path segment leaves nothing to escape with.
 */
public final class StoredFilename {

    /** Anything outside this set becomes an underscore. */
    private static final String SAFE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789-_. ";

    private static final int MAX_LENGTH = 120;

    private StoredFilename() {
    }

    /**
     * @return a name containing no separators, no parent references, and no characters that
     *         mean anything to a filesystem; never blank
     */
    public static String sanitize(String originalFilename) {
        String candidate = lastSegmentOf(originalFilename);

        StringBuilder safe = new StringBuilder(candidate.length());
        for (char character : candidate.toCharArray()) {
            char lower = Character.toLowerCase(character);
            safe.append(SAFE_CHARACTERS.indexOf(lower) >= 0 ? character : '_');
        }

        // A name that is only dots ("." or "..") still refers to a directory.
        String cleaned = safe.toString().strip();
        while (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }

        return cleaned.isBlank() ? "upload-" + UUID.randomUUID() : cleaned;
    }

    /**
     * Keeps only what follows the last separator. Both separators are handled regardless of
     * the host OS, because the name came off the wire and not off this filesystem.
     */
    private static String lastSegmentOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String candidate = originalFilename;
        int separator = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (separator >= 0) {
            candidate = candidate.substring(separator + 1);
        }
        // A Windows drive or stream qualifier survives having no separator ("C:evil.txt").
        int colon = candidate.lastIndexOf(':');
        if (colon >= 0) {
            candidate = candidate.substring(colon + 1);
        }
        return candidate.toLowerCase(Locale.ROOT).isBlank() ? candidate.strip() : candidate.strip();
    }
}
