package com.example.ai_doc.service.nvidia;

/**
 * Pulls a usable JSON object out of a chat-completion response that may also carry
 * reasoning traces, prose commentary, or Markdown fences around it.
 *
 * <p>Shared by every service that asks a model for JSON. Each one having its own copy is
 * how they drift: a fix for a new way the model wraps its answer lands in one and not the
 * others, and the difference only shows up as an intermittent parse failure in production.
 */
public final class ModelJsonResponses {

    private static final String THINK_OPEN_TAG = "<think>";
    private static final String THINK_CLOSE_TAG = "</think>";

    private ModelJsonResponses() {
    }

    /**
     * Returns the first complete, brace-balanced JSON object in the text, or null when there
     * is none.
     *
     * <p>Scanning for a balanced object is what makes this safe: taking everything between
     * the first '{' and the last '}' swallowed any stray brace in the surrounding prose and
     * produced invalid JSON.
     */
    public static String extractJsonObject(String raw) {
        String text = stripReasoningBlocks(raw);

        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char character = text.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }

            if (character == '"') {
                inString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    /**
     * Removes {@code <think>...</think>} scratch work. An unterminated block means the token
     * budget ran out mid-thought, so nothing usable follows it.
     */
    public static String stripReasoningBlocks(String raw) {
        if (raw.indexOf(THINK_OPEN_TAG) < 0) {
            return raw;
        }

        StringBuilder cleaned = new StringBuilder(raw.length());
        int index = 0;

        while (index < raw.length()) {
            int openTag = raw.indexOf(THINK_OPEN_TAG, index);
            if (openTag < 0) {
                cleaned.append(raw, index, raw.length());
                break;
            }

            cleaned.append(raw, index, openTag);
            int closeTag = raw.indexOf(THINK_CLOSE_TAG, openTag);
            if (closeTag < 0) {
                break;
            }
            index = closeTag + THINK_CLOSE_TAG.length();
        }

        return cleaned.toString();
    }
}
