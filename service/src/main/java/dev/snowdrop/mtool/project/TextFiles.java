package dev.snowdrop.mtool.project;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TextFiles {

    private static final Logger logger = Logger.getLogger(TextFiles.class);

    private TextFiles() {
    }

    /**
     * Reads a file as UTF-8, falling back to ISO-8859-1 for legacy encodings (common for JSP, XHTML
     * and FreeMarker files), so that such a file is still inspected instead of being treated as empty.
     */
    public static String read(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            logger.warnf("Could not read %s: %s", file, e.getMessage());
            return "";
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }
}
