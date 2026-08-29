package com.example.ai_doc;

import java.nio.charset.StandardCharsets;

/**
 * Byte payloads that pass upload validation.
 *
 * <p>Validation checks the file's own leading bytes rather than the declared content type,
 * so a fixture of {@code new byte[]{1, 2}} labelled as a PDF is now correctly rejected.
 * Tests that only need "a valid-looking document" use these.
 */
public final class TestFiles {

    private TestFiles() {
    }

    /** A PDF header plus a distinguishing tail, so two fixtures can differ. */
    public static byte[] pdf(String marker) {
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        byte[] tail = marker.getBytes(StandardCharsets.US_ASCII);
        byte[] content = new byte[header.length + tail.length];
        System.arraycopy(header, 0, content, 0, header.length);
        System.arraycopy(tail, 0, content, header.length, tail.length);
        return content;
    }

    public static byte[] pdf() {
        return pdf("test");
    }

    public static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }

    public static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
    }
}
