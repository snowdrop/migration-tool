package dev.snowdrop.mtool.validate.ui;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Thymeleaf to Qute: no Thymeleaf namespace or {@code th:*} attribute remains.
 */
final class ThymeleafChecks implements UiChecks {

    private static final List<Pattern> THYMELEAF = Patterns.caseInsensitive(
            "\\sth:[a-z-]+\\s*=", "<th:", "xmlns:th\\s*=");

    @Override
    public void run(UiContext context) {
        String description = "No Thymeleaf namespaces or attributes remain in templates";
        String passEvidence = "No Thymeleaf attributes found";
        String failLabel = "Thymeleaf attributes";
        context.templatesDoNotMatch("no_thymeleaf_attributes", description, THYMELEAF, passEvidence, failLabel);
    }
}
