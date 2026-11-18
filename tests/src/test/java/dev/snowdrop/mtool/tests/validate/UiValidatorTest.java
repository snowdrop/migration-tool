package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.validate.ui.UiMigrationType;
import dev.snowdrop.mtool.validate.ui.UiValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UiValidatorTest {

    @TempDir
    Path target;

    @TempDir
    Path source;

    @Test
    @DisplayName("Migration type is parsed case-insensitively and rejects unknown values")
    void migrationTypeParsing() {
        assertThat(UiMigrationType.fromValue("JSP-Qute")).isEqualTo(UiMigrationType.JSP_QUTE);
        assertThatThrownBy(() -> UiMigrationType.fromValue("angular"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jsp-qute");
        assertThatThrownBy(() -> UiMigrationType.fromValue(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Several migration types can be given as a comma-separated list")
    void migrationTypeList() {
        assertThat(UiMigrationType.fromValues("thymeleaf-qute, jsp-qute"))
                .containsExactlyInAnyOrder(UiMigrationType.THYMELEAF_QUTE, UiMigrationType.JSP_QUTE);
        assertThat(UiMigrationType.fromValue("freemarker-quarkus")).isEqualTo(UiMigrationType.FREEMARKER_QUARKUS);
        assertThatThrownBy(() -> UiMigrationType.fromValues(" , ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UiMigrationType.fromValues("jsp-qute,nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Validator is named 'ui'")
    void name() {
        assertThat(validator(UiMigrationType.JSP_QUTE).name()).isEqualTo("ui");
    }

    @Test
    @DisplayName("JSP to Qute: clean project passes all checks")
    void jspQuteClean() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html", "<h1>{title}</h1>");
        write("src/main/resources/application.properties", "quarkus.qute.strict-rendering=true");
        write("src/main/java/com/example/IndexResource.java", "@Path(\"/\") public class IndexResource {}");
        write(source, "src/main/webapp/WEB-INF/views/index.jsp", "<h1>${title}</h1>");

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertThat(report.failed()).isZero();
        assertResult(report, "no_jsp_files", Status.PASSED);
        assertResult(report, "quarkus_rest_qute_dependency", Status.PASSED);
        assertResult(report, "source_target_parity", Status.PASSED);
    }

    @Test
    @DisplayName("JSP to Qute: detects remaining JSP files, JSTL tags, scriptlets, form tags and implicit objects")
    void jspRemnants() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute")
                .replace("</dependencies>", dependency("org.apache.tomcat.embed", "tomcat-embed-jasper")
                        + "</dependencies>"));
        write("src/main/webapp/index.jsp", "<html/>");
        write("src/main/webapp/tags/row.tag", "<tr/>");
        write("src/main/resources/templates/list.html",
                "<c:forEach items=\"${items}\" var=\"i\">${param.name}</c:forEach>"
                        + "<% int x = 1; %><form:input path=\"name\"/>");

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "no_jsp_files", Status.FAILED);
        assertEvidence(report, "no_jsp_files", ".tag");
        assertResult(report, "no_jstl_tags", Status.FAILED);
        assertResult(report, "no_jsp_scriptlets", Status.FAILED);
        assertResult(report, "no_spring_form_tags", Status.FAILED);
        assertResult(report, "no_jsp_implicit_objects", Status.FAILED);
        assertResult(report, "legacy_dependencies_removed", Status.FAILED);
        assertEvidence(report, "legacy_dependencies_removed", "tomcat-embed-jasper");
        assertResult(report, "expression_language_migration", Status.FAILED);
        assertResult(report, "static_resources_location", Status.PASSED);
    }

    @Test
    @DisplayName("Thymeleaf to Qute: detects th: attributes, legacy dependency and CSRF tokens")
    void thymeleafRemnants() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute")
                .replace("</dependencies>", dependency("org.thymeleaf", "thymeleaf") + "</dependencies>"));
        write("src/main/resources/templates/index.html",
                "<p th:text=\"${name}\">x</p><input name=\"_csrf\"/>");
        write("src/main/resources/templates/other.html",
                "<html xmlns:th=\"http://www.thymeleaf.org\"><div th:aria-label=\"${a}\"/></html>");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "no_thymeleaf_attributes", Status.FAILED);
        assertEvidence(report, "no_thymeleaf_attributes", "2 files");
        assertResult(report, "legacy_dependencies_removed", Status.FAILED);
        assertEvidence(report, "legacy_dependencies_removed", "org.thymeleaf:thymeleaf");
        assertResult(report, "no_csrf_tokens", Status.FAILED);
    }

    @Test
    @DisplayName("FreeMarker to Qute: detects ftl files and directives")
    void freemarkerRemnants() throws IOException {
        write("pom.xml", pomWith("quarkus-qute"));
        write("src/main/resources/templates/page.ftl", "<#if x>y</#if>");
        write("src/main/resources/templates/page.html", "<@alert message=\"x\"/>");
        write("src/main/java/com/example/Cfg.java",
                "import freemarker.template.Configuration;\npublic class Cfg { FreeMarkerConfigurer c; }");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUTE).validate(target);

        assertResult(report, "no_freemarker_files", Status.FAILED);
        assertResult(report, "no_freemarker_directives", Status.FAILED);
        assertResult(report, "no_freemarker_java", Status.FAILED);
        assertResult(report, "quarkus_rest_qute_dependency", Status.WARNING);
        assertEvidence(report, "quarkus_rest_qute_dependency", "without quarkus-rest-qute");
    }

    @Test
    @DisplayName("JSF to Qute: detects xhtml files, components and managed beans")
    void jsfQuteRemnants() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/webapp/index.xhtml", "<html/>");
        write("src/main/resources/templates/index.html", "<h:form/>");
        write("src/main/java/com/example/Bean.java", "@ManagedBean public class Bean {}");
        write("src/main/webapp/WEB-INF/faces-config.xml", "<faces-config/>");
        write("src/main/webapp/WEB-INF/web.xml", "<web-app><servlet-class>jakarta.faces.webapp.FacesServlet"
                + "</servlet-class></web-app>");

        ValidationReport report = validator(UiMigrationType.JSF_QUTE).validate(target);

        assertResult(report, "no_jsf_xhtml_files", Status.FAILED);
        assertResult(report, "no_jsf_components", Status.FAILED);
        assertResult(report, "no_jsf_managed_beans", Status.FAILED);
        assertResult(report, "no_faces_config", Status.FAILED);
        assertEvidence(report, "no_faces_config", "faces-config.xml");
    }

    @Test
    @DisplayName("JSF annotations such as @ViewScoped and @FacesConverter are rejected for JSF to Qute")
    void jsfQuteAnnotations() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/java/com/example/Conv.java", "@FacesConverter(\"c\") public class Conv {}");

        ValidationReport report = validator(UiMigrationType.JSF_QUTE).validate(target);

        assertResult(report, "no_jsf_managed_beans", Status.FAILED);
    }

    @Test
    @DisplayName("Managed bean marker inside a comment is ignored")
    void commentsAreIgnored() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/java/com/example/Bean.java", "// was @ManagedBean\npublic class Bean {}");

        ValidationReport report = validator(UiMigrationType.JSF_QUTE).validate(target);

        assertResult(report, "no_jsf_managed_beans", Status.PASSED);
    }

    @Test
    @DisplayName("Qute: missing build file and templates fail the structural checks")
    void quteEmptyProject() {
        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "quarkus_rest_qute_dependency", Status.FAILED);
        assertResult(report, "qute_template_structure", Status.FAILED);
    }

    @Test
    @DisplayName("Qute: source/target parity fails when most views are missing")
    void parityFails() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/one.html", "x");
        for (int i = 0; i < 4; i++) {
            write(source, "src/main/webapp/WEB-INF/views/v" + i + ".jsp", "x");
        }

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "source_target_parity", Status.FAILED);
    }

    @Test
    @DisplayName("The source project is required")
    void sourceProjectIsRequired() {
        assertThatThrownBy(() -> new UiValidator(UiMigrationType.JSP_QUTE, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source project not found");
        assertThatThrownBy(() -> new UiValidator(UiMigrationType.JSP_QUTE, source.resolve("missing"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("JSF to MyFaces: valid project passes")
    void myFacesClean() throws IOException {
        write("pom.xml", pomWith("quarkus-rest").replace("</dependencies>",
                dependency("org.apache.myfaces.core.extensions.quarkus", "myfaces-quarkus") + "</dependencies>"));
        write("src/main/resources/META-INF/resources/index.xhtml",
                "<html xmlns:h=\"jakarta.faces.html\"></html>");
        write("src/main/resources/META-INF/web.xml",
                "<web-app><servlet><servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class></servlet></web-app>");
        write("src/main/resources/META-INF/faces-config.xml",
                "<faces-config xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"4.0\"/>");
        write("src/main/java/com/example/Bean.java",
                "import jakarta.inject.Named;\n@Named @ViewScoped public class Bean implements Serializable {}");
        write(source, "src/main/webapp/index.xhtml", "<html/>");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertThat(report.failed()).isZero();
        assertResult(report, "myfaces_dependency", Status.PASSED);
        assertResult(report, "web_xml_faces_servlet", Status.PASSED);
        assertResult(report, "faces_config_jakarta", Status.PASSED);
        assertResult(report, "primefaces_extension", Status.PASSED);
        assertResult(report, "source_target_parity", Status.PASSED);
    }

    @Test
    @DisplayName("JSF to MyFaces: detects javax namespaces, legacy beans and misplaced xhtml")
    void myFacesProblems() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/webapp/index.xhtml", "<html xmlns:h=\"http://xmlns.jcp.org/jsf/html\"/>");
        write("src/main/resources/META-INF/faces-config.xml",
                "<faces-config xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"2.3\"/>");
        write("src/main/java/com/example/Bean.java",
                "import javax.faces.bean.ManagedBean;\n@ManagedBean @ViewScoped public class Bean {}");
        write("src/main/java/com/example/Conv.java",
                "@FacesConverter(\"c\") public class Conv { @Inject Svc s; }");
        write("src/main/java/com/example/Scoped.java",
                "@SessionScope public class Scoped { @Inject FacesContext context; }");
        write(source, "a.xhtml", "x");
        write(source, "b.xhtml", "x");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "myfaces_dependency", Status.FAILED);
        assertResult(report, "web_xml_faces_servlet", Status.FAILED);
        assertResult(report, "no_spring_scope_annotations", Status.FAILED);
        assertResult(report, "no_faces_context_injection", Status.FAILED);
        assertResult(report, "jakarta_faces_namespaces", Status.FAILED);
        assertResult(report, "faces_config_jakarta", Status.FAILED);
        assertResult(report, "cdi_named_beans", Status.FAILED);
        assertResult(report, "converter_validator_managed", Status.FAILED);
        assertResult(report, "viewscoped_serializable", Status.FAILED);
        assertResult(report, "xhtml_resources_location", Status.FAILED);
        assertResult(report, "javax_jakarta_imports", Status.FAILED);
        assertResult(report, "source_target_parity", Status.FAILED);
    }

    @Test
    @DisplayName("JSF to MyFaces: Spring references in faces-config.xml fail, PrimeFaces needs its extension")
    void myFacesSpringAndPrimeFaces() throws IOException {
        write("pom.xml", pomWith("quarkus-rest").replace("</dependencies>",
                dependency("org.apache.myfaces.core.extensions.quarkus", "myfaces-quarkus") + "</dependencies>"));
        write("src/main/resources/META-INF/faces-config.xml",
                "<faces-config xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"4.0\">"
                        + "<el-resolver>org.springframework.web.jsf.el.SpringBeanFacesELResolver</el-resolver>"
                        + "</faces-config>");
        write("src/main/resources/META-INF/resources/index.xhtml",
                "<html xmlns:p=\"http://primefaces.org/ui\"/>");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "faces_config_jakarta", Status.FAILED);
        assertEvidence(report, "faces_config_jakarta", "Spring");
        assertResult(report, "primefaces_extension", Status.FAILED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: clean project passes")
    void freemarkerQuarkusClean() throws IOException {
        write("pom.xml", pomWith("quarkus-rest").replace("</dependencies>",
                dependency("io.quarkiverse.freemarker", "quarkus-freemarker") + "</dependencies>"));
        write("src/main/resources/freemarker/templates/products.ftl", "<#list products as p>${p.name}</#list>");
        write("src/main/resources/application.properties", "quarkus.freemarker.default-encoding=UTF-8");
        write("src/main/java/com/example/ProductResource.java",
                "@Path(\"/p\") public class ProductResource { @Inject freemarker.template.Configuration cfg;"
                        + " String list() { cfg.getTemplate(\"products.ftl\").process(model, writer); } }");
        write(source, "src/main/resources/templates/products.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertThat(report.failed()).isZero();
        assertResult(report, "quarkus_freemarker_dependency", Status.PASSED);
        assertResult(report, "freemarker_template_set_config", Status.PASSED);
        assertResult(report, "freemarker_templates_location", Status.PASSED);
        assertEvidence(report, "freemarker_templates_location", "src/main/resources/freemarker/templates");
        assertResult(report, "freemarker_configuration_injection", Status.PASSED);
        assertResult(report, "source_target_parity", Status.PASSED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: detects Spring leftovers and misplaced templates")
    void freemarkerQuarkusProblems() throws IOException {
        write("pom.xml", pomWith("quarkus-rest").replace("</dependencies>",
                dependency("org.springframework.boot", "spring-boot-starter-freemarker") + "</dependencies>"));
        write("src/main/webapp/legacy.ftl", "<#import \"/spring.ftl\" as spring><@spring.message \"k\"/>");
        write("src/main/resources/application.properties", "spring.freemarker.cache=true");
        write("src/main/java/com/example/Ctl.java",
                "public class Ctl { FreeMarkerConfigurer c; ModelAndView mv; }");
        write(source, "src/main/resources/templates/a.ftl", "x");
        write(source, "src/main/resources/templates/b.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "quarkus_freemarker_dependency", Status.FAILED);
        assertResult(report, "legacy_dependencies_removed", Status.FAILED);
        assertResult(report, "freemarker_templates_location", Status.FAILED);
        assertResult(report, "no_spring_freemarker_properties", Status.FAILED);
        assertResult(report, "no_spring_macros", Status.FAILED);
        assertResult(report, "no_spring_freemarker_beans", Status.FAILED);
        assertResult(report, "no_model_and_view", Status.FAILED);
        assertResult(report, "freemarker_configuration_injection", Status.FAILED);
        assertResult(report, "source_target_parity", Status.FAILED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: spring.freemarker YAML configuration is detected")
    void freemarkerQuarkusYaml() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.yml", "spring:\n  freemarker:\n    cache: true\n");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "no_spring_freemarker_properties", Status.FAILED);
    }

    @Test
    @DisplayName("Qute: @CheckedTemplate templates must exist at the class-named location")
    void checkedTemplateLocations() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/java/com/example/TodoResource.java",
                "@Path(\"/t\") public class TodoResource {\n"
                        + "  @CheckedTemplate static class Templates {\n"
                        + "    static native TemplateInstance todos(List<String> t);\n"
                        + "    static native TemplateInstance todoDetail(String t);\n"
                        + "  }\n}");
        write("src/main/resources/templates/TodoResource/todos.html", "x");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "checked_template_locations", Status.FAILED);
        assertEvidence(report, "checked_template_locations", "TodoResource/todoDetail.html");

        write("src/main/resources/templates/TodoResource/todoDetail.html", "x");
        assertResult(validator(UiMigrationType.THYMELEAF_QUTE).validate(target), "checked_template_locations",
                Status.PASSED);
    }

    @Test
    @DisplayName("Qute: top-level @CheckedTemplate uses the templates root and basePath is honored")
    void checkedTemplateTopLevelAndBasePath() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/java/com/example/Templates.java",
                "@CheckedTemplate public class Templates { static native TemplateInstance index(); }");
        write("src/main/java/com/example/Mail.java",
                "@CheckedTemplate(basePath = \"mail\") public class Mail { static native TemplateInstance welcome(); }");
        write("src/main/resources/templates/index.html", "x");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "checked_template_locations", Status.FAILED);
        assertEvidence(report, "checked_template_locations", "mail/welcome.html");
    }

    @Test
    @DisplayName("Qute: {msg:...} keys require a @MessageBundle interface")
    void messageBundle() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html", "<h1>{msg:welcome}</h1>");

        assertResult(validator(UiMigrationType.JSP_QUTE).validate(target), "qute_message_bundle", Status.FAILED);

        write("src/main/java/com/example/AppMessages.java",
                "@MessageBundle public interface AppMessages { @Message(\"hi\") String welcome(); }");
        assertResult(validator(UiMigrationType.JSP_QUTE).validate(target), "qute_message_bundle", Status.PASSED);
    }

    @Test
    @DisplayName("Qute: templates without any JAX-RS resource fail")
    void jaxRsEndpoints() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html", "x");

        assertResult(validator(UiMigrationType.JSP_QUTE).validate(target), "jax_rs_endpoints", Status.FAILED);

        write("src/main/java/com/example/Res.java", "@Path(\"/\") public class Res {}");
        assertResult(validator(UiMigrationType.JSP_QUTE).validate(target), "jax_rs_endpoints", Status.PASSED);
    }

    @Test
    @DisplayName("Mixed technologies: path-specific rules are qualified with the migration type")
    void multipleTypes() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/webapp/index.jsp", "<html/>");
        write("src/main/resources/templates/page.html", "<p th:text=\"${x}\"/>");

        ValidationReport report = new UiValidator(
                Set.of(UiMigrationType.JSP_QUTE, UiMigrationType.THYMELEAF_QUTE), source, true).validate(target);

        assertResult(report, "no_jsp_files (jsp-qute)", Status.FAILED);
        assertResult(report, "no_thymeleaf_attributes (thymeleaf-qute)", Status.FAILED);
        assertResult(report, "quarkus_rest_qute_dependency", Status.PASSED);
        assertThat(report.getResults().stream().filter(r -> r.rule().name().startsWith("quarkus_rest_qute_dependency")))
                .hasSize(1);
    }

    @Test
    @DisplayName("CSRF tokens are detected in FreeMarker templates and in the shared checks of every type")
    void csrfInFreemarkerTemplates() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/freemarker/templates/form.ftl",
                "<input name=\"${_csrf.parameterName}\" value=\"${_csrf.token}\">");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "no_csrf_tokens", Status.FAILED);
    }

    @Test
    @DisplayName("Qute: parity is a warning when the source has no view of the selected type")
    void parityWithoutSourceViews() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write(source, "src/main/resources/templates/index.html", "<p th:text=\"${x}\"/>");

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "source_target_parity", Status.WARNING);
        assertEvidence(report, "source_target_parity", "Verify the UI migration type");
    }

    @Test
    @DisplayName("Qute: Thymeleaf parity only counts the views of the source templates directory")
    void thymeleafParityIgnoresStaticHtml() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html", "x");
        write(source, "src/main/resources/templates/index.html", "x");
        for (int i = 0; i < 4; i++) {
            write(source, "src/main/resources/static/page" + i + ".html", "x");
        }

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "source_target_parity", Status.PASSED);
        assertEvidence(report, "source_target_parity", "1 source → 1 target");
    }

    @Test
    @DisplayName("Qute: JavaScript template literals in inline scripts are not reported as legacy EL")
    void inlineScriptsIgnoredByElCheck() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html",
                "<h1>{title}</h1><script>const msg = `Hello ${name}`;</script>");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "expression_language_migration", Status.PASSED);
    }

    @Test
    @DisplayName("A leftover JSTL tag only fails the JSTL rule, not the expression language rule")
    void noDuplicateFailures() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/resources/templates/index.html", "<c:if test=\"{x}\">y</c:if>");

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "no_jstl_tags", Status.FAILED);
        assertResult(report, "expression_language_migration", Status.PASSED);
    }

    @Test
    @DisplayName("Files with a legacy encoding are inspected instead of being treated as empty")
    void latin1FilesAreRead() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        Path template = target.resolve("src/main/resources/templates/index.html");
        Files.createDirectories(template.getParent());
        Files.write(template, "<p th:text=\"${caf\u00e9}\">Caf\u00e9</p>".getBytes(StandardCharsets.ISO_8859_1));

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "no_thymeleaf_attributes", Status.FAILED);
    }

    @Test
    @DisplayName("Dependencies only count when declared: exclusions, dependencyManagement and comments are ignored")
    void dependencyLookupIgnoresNonDeclaredEntries() throws IOException {
        write("pom.xml", "<project>"
                + "<dependencyManagement><dependencies>" + dependency("org.thymeleaf", "thymeleaf")
                + "</dependencies></dependencyManagement>"
                + "<dependencies>" + dependency("io.quarkus", "quarkus-rest-qute")
                + "<dependency><groupId>org.example</groupId><artifactId>lib</artifactId><exclusions>"
                + "<exclusion><groupId>org.thymeleaf</groupId><artifactId>thymeleaf-spring6</artifactId></exclusion>"
                + "</exclusions></dependency>"
                + "<!-- " + dependency("org.thymeleaf.extras", "thymeleaf-extras-springsecurity6") + " -->"
                + "</dependencies></project>");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "legacy_dependencies_removed", Status.PASSED);
        assertResult(report, "quarkus_rest_qute_dependency", Status.PASSED);
    }

    @Test
    @DisplayName("Dependencies of sub-module poms are taken into account")
    void dependencyLookupIncludesModules() throws IOException {
        write("pom.xml", "<project><modules><module>web</module></modules></project>");
        write("web/pom.xml", pomWith("quarkus-rest-qute"));

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "quarkus_rest_qute_dependency", Status.PASSED);
    }

    @Test
    @DisplayName("Gradle: a dependency does not match another artifact sharing its prefix")
    void gradleDependencyExactMatch() throws IOException {
        write("build.gradle", "dependencies {\n  implementation 'io.quarkus:quarkus-qute-web'\n}\n");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "quarkus_rest_qute_dependency", Status.FAILED);

        write("build.gradle", "dependencies {\n  implementation(\"io.quarkus:quarkus-rest-qute:3.15.1\")\n}\n");
        assertResult(validator(UiMigrationType.THYMELEAF_QUTE).validate(target), "quarkus_rest_qute_dependency",
                Status.PASSED);
    }

    @Test
    @DisplayName("FreeMarker to Qute: the Quarkiverse FreeMarker extension is a legacy dependency")
    void freemarkerQuteRejectsQuarkiverseExtension() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute").replace("</dependencies>",
                dependency("io.quarkiverse.freemarker", "quarkus-freemarker") + "</dependencies>"));

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUTE).validate(target);

        assertResult(report, "legacy_dependencies_removed", Status.FAILED);
        assertEvidence(report, "legacy_dependencies_removed", "io.quarkiverse.freemarker:quarkus-freemarker");
    }

    @Test
    @DisplayName("JSF to MyFaces: a faces-config.xml with a pre-4.0 version fails even with the Jakarta namespace")
    void facesConfigVersion() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/META-INF/faces-config.xml",
                "<faces-config xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"3.0\"/>");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "faces_config_jakarta", Status.FAILED);
        assertEvidence(report, "faces_config_jakarta", "version 4.0");
    }

    @Test
    @DisplayName("JSF to MyFaces: @ManagedBean fails even when @Named is also present")
    void managedBeanWithNamed() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/java/com/example/Bean.java", "@Named @ManagedBean public class Bean {}");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "cdi_named_beans", Status.FAILED);
    }

    @Test
    @DisplayName("JSF to MyFaces: Java checks ignore test sources")
    void myFacesIgnoresTestSources() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/test/java/com/example/Fixture.java", "@SessionScope public class Fixture {}");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "no_spring_scope_annotations", Status.PASSED);
    }

    @Test
    @DisplayName("A view technology cannot be both migrated to Qute and preserved")
    void conflictingTypesAreRejected() {
        assertThatThrownBy(() -> new UiValidator(
                Set.of(UiMigrationType.JSF_QUTE, UiMigrationType.JSF_MYFACES), source, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be combined");
        assertThatThrownBy(() -> new UiValidator(
                Set.of(UiMigrationType.FREEMARKER_QUTE, UiMigrationType.FREEMARKER_QUARKUS), source, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Skipping the build is reported as a warning")
    void skippedBuildIsReported() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertResult(report, "quarkus_build", Status.WARNING);
    }

    @Test
    @DisplayName("Build: the Maven wrapper is used and the [ERROR] lines are reported on failure")
    void buildFailureReportsErrors() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        writeExecutable("mvnw", "#!/bin/sh\necho '[INFO] Downloading something'\n"
                + "echo '[ERROR] Template not found: index.html'\necho '[ERROR] BUILD FAILURE'\nexit 1\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.FAILED);
        assertEvidence(report, "quarkus_build", "./mvnw clean package");
        assertEvidence(report, "quarkus_build", "Template not found: index.html");
        assertThat(find(report, "quarkus_build").evidence()).doesNotContain("Downloading");
    }

    @Test
    @DisplayName("Build: a successful build passes")
    void buildSuccess() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        writeExecutable("mvnw", "#!/bin/sh\nexit 0\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.PASSED);
    }

    @Test
    @DisplayName("A Java package named 'build' is scanned, only build output directories are excluded")
    void buildPackageIsScanned() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/java/com/acme/build/Bean.java", "@SessionScope public class Bean {}");
        write("target/classes/Generated.java", "@SessionScope public class Generated {}");

        ValidationReport report = validator(UiMigrationType.JSF_MYFACES).validate(target);

        assertResult(report, "no_spring_scope_annotations", Status.FAILED);
        assertEvidence(report, "no_spring_scope_annotations", "com/acme/build/Bean.java");
        assertThat(find(report, "no_spring_scope_annotations").evidence()).doesNotContain("Generated");
    }

    @Test
    @DisplayName("URLs in Java string literals are not mistaken for comments")
    void urlsInStringsAreKept() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("src/main/java/com/example/TodoResource.java",
                "@Path(\"/t\") public class TodoResource {\n"
                        + "  static final String DOC = \"https://quarkus.io/guides/qute { see\";\n"
                        + "  @CheckedTemplate static class Templates {\n"
                        + "    static native TemplateInstance todos();\n"
                        + "  }\n}");

        ValidationReport report = validator(UiMigrationType.THYMELEAF_QUTE).validate(target);

        assertResult(report, "checked_template_locations", Status.FAILED);
        assertEvidence(report, "checked_template_locations", "TodoResource/todos.html");
    }

    @Test
    @DisplayName("Evidence lists at most 3 files and says how many more there are")
    void evidenceSampleSize() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        for (int i = 0; i < 5; i++) {
            write("src/main/webapp/v" + i + ".jsp", "x");
        }

        ValidationReport report = validator(UiMigrationType.JSP_QUTE).validate(target);

        assertEvidence(report, "no_jsp_files", "Found 5 JSP files");
        assertEvidence(report, "no_jsp_files", "(and 2 more)");
    }

    @Test
    @DisplayName("Legacy dependency and expression checks only run for the paths that define them")
    void dataDrivenChecksPerType() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));

        ValidationReport freemarker = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);
        assertResult(freemarker, "legacy_dependencies_removed", Status.PASSED);
        assertNoResult(freemarker, "expression_language_migration");

        ValidationReport myFaces = validator(UiMigrationType.JSF_MYFACES).validate(target);
        assertNoResult(myFaces, "legacy_dependencies_removed");
        assertNoResult(myFaces, "expression_language_migration");

        ValidationReport mixed = new UiValidator(
                Set.of(UiMigrationType.JSP_QUTE, UiMigrationType.THYMELEAF_QUTE), source, true).validate(target);
        assertResult(mixed, "legacy_dependencies_removed (jsp-qute)", Status.PASSED);
        assertResult(mixed, "expression_language_migration (thymeleaf-qute)", Status.PASSED);
    }

    @Test
    @DisplayName("Build: a build that cannot be started fails, since the templates were not validated")
    void buildNotRunnableFails() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        writeExecutable("mvnw", "#!/nonexistent/interpreter\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.FAILED);
        assertEvidence(report, "quarkus_build", "Could not run ./mvnw");
    }

    @Test
    @DisplayName("Build: a Gradle project is built with the Gradle wrapper")
    void gradleBuild() throws IOException {
        write("build.gradle", "dependencies {\n  implementation 'io.quarkus:quarkus-rest-qute'\n}\n");
        writeExecutable("gradlew", "#!/bin/sh\necho \"$@\" > gradle-args.txt\nexit 0\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.PASSED);
        assertEvidence(report, "quarkus_build", "./gradlew clean build -x test");
        assertThat(Files.readString(target.resolve("gradle-args.txt")).trim()).isEqualTo("clean build -x test");
    }

    @Test
    @DisplayName("Build: a failing Gradle build reports the 'What went wrong' line")
    void gradleBuildFailure() throws IOException {
        write("build.gradle", "dependencies {}\n");
        writeExecutable("gradlew", "#!/bin/sh\necho 'FAILURE: Build failed with an exception.'\n"
                + "echo '* What went wrong:'\necho \"Execution failed for task ':quarkusBuild'.\"\nexit 1\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.FAILED);
        assertEvidence(report, "quarkus_build", "Execution failed for task ':quarkusBuild'.");
    }

    @Test
    @DisplayName("Build: Maven wins when the project has both a pom.xml and a Gradle build file")
    void buildPrefersMaven() throws IOException {
        write("pom.xml", pomWith("quarkus-rest-qute"));
        write("build.gradle", "dependencies {}\n");
        writeExecutable("mvnw", "#!/bin/sh\nexit 0\n");
        writeExecutable("gradlew", "#!/bin/sh\nexit 1\n");

        ValidationReport report = new UiValidator(UiMigrationType.JSP_QUTE, source, false).validate(target);

        assertResult(report, "quarkus_build", Status.PASSED);
        assertEvidence(report, "quarkus_build", "./mvnw clean package");
        // Dependencies are read from the pom too
        assertResult(report, "quarkus_rest_qute_dependency", Status.PASSED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: templates in the Spring 'templates' folder are misplaced with the default template set")
    void freemarkerQuarkusDefaultLocation() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/templates/products.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_templates_location", Status.FAILED);
        assertEvidence(report, "freemarker_templates_location", "outside src/main/resources/freemarker/templates");
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: base-path with includes selects the template directory")
    void freemarkerQuarkusCustomBasePath() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.properties",
                "quarkus.freemarker.base-path=templates\nquarkus.freemarker.includes=**\n");
        write("src/main/resources/templates/products.ftl", "x");
        write(source, "src/main/resources/templates/products.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_template_set_config", Status.PASSED);
        assertResult(report, "freemarker_templates_location", Status.PASSED);
        assertResult(report, "source_target_parity", Status.PASSED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: base-path without includes fails, as it fails the Quarkus build")
    void freemarkerQuarkusBasePathWithoutIncludes() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.properties", "quarkus.freemarker.base-path=templates");
        write("src/main/resources/templates/products.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_template_set_config", Status.FAILED);
        assertEvidence(report, "freemarker_template_set_config", "quarkus.freemarker.includes");
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: the template set is read from application.yaml")
    void freemarkerQuarkusYamlTemplateSet() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.yaml",
                "quarkus:\n  freemarker:\n    base-path: views\n    includes: \"**/*.ftl\"\n");
        write("src/main/resources/views/products.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_template_set_config", Status.PASSED);
        assertResult(report, "freemarker_templates_location", Status.PASSED);
        assertEvidence(report, "freemarker_templates_location", "src/main/resources/views");
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: an @TemplatePath Template injection renders the templates")
    void freemarkerQuarkusTemplatePath() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/freemarker/templates/hello.ftl", "Hello ${name}!");
        write("src/main/java/com/example/HelloResource.java",
                "import io.quarkiverse.freemarker.TemplatePath;\n@Path(\"hello\") public class HelloResource {"
                        + " @Inject @TemplatePath(\"hello.ftl\") Template hello;"
                        + " String hello() { hello.process(Map.of(\"name\", \"bob\"), writer); } }");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_configuration_injection", Status.PASSED);
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: includes without base-path looks up templates from the resources root")
    void freemarkerQuarkusIncludesOnly() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.properties", "quarkus.freemarker.includes=**/*.ftl");
        write("src/main/resources/mail/welcome.ftl", "x");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "freemarker_template_set_config", Status.PASSED);
        assertResult(report, "freemarker_templates_location", Status.PASSED);
        assertEvidence(report, "freemarker_templates_location", "1 FreeMarker templates in src/main/resources");
    }

    @Test
    @DisplayName("FreeMarker with Quarkus: spring.freemarker properties are detected in profile files too")
    void freemarkerQuarkusSpringPropertiesInProfiles() throws IOException {
        write("pom.xml", pomWith("quarkus-rest"));
        write("src/main/resources/application.properties", "# spring.freemarker.cache was migrated\n");
        write("src/main/resources/application-dev.properties", "spring.freemarker.cache=false\n");

        ValidationReport report = validator(UiMigrationType.FREEMARKER_QUARKUS).validate(target);

        assertResult(report, "no_spring_freemarker_properties", Status.FAILED);
        assertEvidence(report, "no_spring_freemarker_properties", "application-dev.properties");
        assertThat(find(report, "no_spring_freemarker_properties").evidence())
                .doesNotContain("application.properties");
    }

    // -- helpers --------------------------------------------------------

    private void assertNoResult(ValidationReport report, String ruleName) {
        assertThat(report.getResults()).noneMatch(r -> r.rule().name().equals(ruleName));
    }

    private void writeExecutable(String relativePath, String content) throws IOException {
        write(relativePath, content);
        assertThat(target.resolve(relativePath).toFile().setExecutable(true)).isTrue();
    }

    private UiValidator validator(UiMigrationType type) {
        return new UiValidator(type, source, true);
    }

    private void write(String relativePath, String content) throws IOException {
        write(target, relativePath, content);
    }

    private void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String dependency(String groupId, String artifactId) {
        return "<dependency><groupId>" + groupId + "</groupId><artifactId>" + artifactId
                + "</artifactId></dependency>";
    }

    private static String pomWith(String quarkusArtifact) {
        return "<project><dependencies>" + dependency("io.quarkus", quarkusArtifact) + "</dependencies></project>";
    }

    private void assertResult(ValidationReport report, String ruleName, Status expected) {
        ValidationResult result = find(report, ruleName);
        assertThat(result.status())
                .as("Rule '%s' evidence: %s", ruleName, result.evidence())
                .isEqualTo(expected);
    }

    private void assertEvidence(ValidationReport report, String ruleName, String expectedSubstring) {
        assertThat(find(report, ruleName).evidence()).contains(expectedSubstring);
    }

    private ValidationResult find(ValidationReport report, String ruleName) {
        return report.getResults().stream()
                .filter(r -> r.rule().name().equals(ruleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for rule: " + ruleName));
    }
}
