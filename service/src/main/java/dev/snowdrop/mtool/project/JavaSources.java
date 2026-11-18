package dev.snowdrop.mtool.project;

/**
 * Lightweight helpers for the text-based inspection of Java sources.
 */
// TODO: if we use the parser, we won't need this
public final class JavaSources {

    private JavaSources() {
    }

    /**
     * Removes line and block comments, keeping string, text block and character literals intact,
     * so that e.g. {@code "http://example.com"} is not truncated.
     */
    public static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int length = source.length();
        while (i < length) {
            char c = source.charAt(i);
            char next = i + 1 < length ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
                out.append(' ');
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                int stop = end < 0 ? length : end + 3;
                out.append(source, i, stop);
                i = stop;
            } else if (c == '"' || c == '\'') {
                int stop = endOfLiteral(source, i, c);
                out.append(source, i, stop);
                i = stop;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int endOfLiteral(String source, int start, char quote) {
        int i = start + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote || c == '\n') {
                return i + 1;
            } else {
                i++;
            }
        }
        return source.length();
    }

    /** Index of the brace closing the one at {@code openIndex}, or the end of the content. */
    public static int closingBrace(String content, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return content.length();
    }
}
