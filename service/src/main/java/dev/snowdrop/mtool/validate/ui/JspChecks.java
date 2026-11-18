package dev.snowdrop.mtool.validate.ui;

import java.util.List;
import java.util.regex.Pattern;

/**
 * JSP to Qute: no JSP file, JSTL tag, scriptlet, Spring form tag or JSP implicit object remains.
 */
final class JspChecks implements UiChecks {

    private static final List<Pattern> JSTL = Patterns.caseInsensitive(
            "<c:", "<fmt:", "<fn:", "\\$\\{fn:", "<%@\\s*taglib");
    private static final List<Pattern> SCRIPTING = List.of(
            Pattern.compile("<%"),
            Pattern.compile("<jsp:", Pattern.CASE_INSENSITIVE));
    private static final List<Pattern> SPRING_FORM_TAGS = Patterns.caseInsensitive("<form:", "<spring:");
    private static final List<Pattern> IMPLICIT_OBJECTS = List.of(Pattern.compile(
            "\\$\\{\\s*(pageContext|pageScope|requestScope|sessionScope|applicationScope|param"
                    + "|paramValues|header|headerValues|cookie|initParam)\\s*[.\\[}]"));

    @Override
    public void run(UiContext context) {
        context.noFiles("no_jsp_files", "No JSP files remain", "JSP", List.of(".jsp", ".jspx", ".tag"));
        context.templatesDoNotMatch("no_jstl_tags", "No JSTL tags remain in templates",
                JSTL, "No JSTL tags found", "JSTL tags");
        context.templatesDoNotMatch("no_jsp_scriptlets",
                "No JSP directives, scriptlets or jsp: actions remain in templates",
                SCRIPTING, "No JSP directives or scriptlets found", "JSP directives/scriptlets");
        context.templatesDoNotMatch("no_spring_form_tags", "Spring form tags must be converted to plain HTML in templates",
                SPRING_FORM_TAGS, "No Spring form tags found", "Spring form tags");
        context.templatesDoNotMatch("no_jsp_implicit_objects", "No JSP implicit objects remain in templates",
                IMPLICIT_OBJECTS, "No JSP implicit objects found", "JSP implicit objects");
    }
}
