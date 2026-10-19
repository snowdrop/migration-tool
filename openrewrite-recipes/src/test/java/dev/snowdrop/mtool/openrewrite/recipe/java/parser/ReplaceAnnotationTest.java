package dev.snowdrop.mtool.openrewrite.recipe.java.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReplaceAnnotationTest {

    @Test
    void replaceMarkerAnnotation() {
        String source = """
                public class HomeCinema {
                    @Deprecated
                    public void display() {
                        System.out.println("old");
                    }
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source, "@Deprecated",
                "@SuppressWarnings(\"deprecation\")");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@SuppressWarnings(\"deprecation\")");
        assertThat(result.source()).doesNotContain("@Deprecated");
    }

    @Test
    void replaceAnnotationWithFullyQualifiedPattern() {
        String source = """
                import org.jetbrains.annotations.NotNull;

                public class Service {
                    @NotNull
                    public String getName() {
                        return "name";
                    }
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@org.jetbrains.annotations.NotNull",
                "@jakarta.annotation.Nonnull");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@Nonnull");
        assertThat(result.source()).doesNotContain("@NotNull");
        assertThat(result.source()).contains("import jakarta.annotation.Nonnull;");
        assertThat(result.source()).doesNotContain("import org.jetbrains.annotations.NotNull;");
    }

    @Test
    void replaceMultipleOccurrences() {
        String source = """
                public class Controller {
                    @Deprecated
                    public void handle() {}

                    @Deprecated
                    public void legacy() {}
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source, "@Deprecated", "@Override");

        assertThat(result.replacementCount()).isEqualTo(2);
        assertThat(result.source()).doesNotContain("@Deprecated");
    }

    @Test
    void replaceSingleMemberAnnotation() {
        String source = """
                public class Calc {
                    @SuppressWarnings("unchecked")
                    public void compute() {}
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@SuppressWarnings",
                "@SuppressWarnings(\"all\")");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@SuppressWarnings(\"all\")");
        assertThat(result.source()).doesNotContain("\"unchecked\"");
    }

    @Test
    void noMatchReturnsZeroReplacements() {
        String source = """
                public class Plain {
                    public void doSomething() {}
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source, "@Deprecated", "@Override");

        assertThat(result.replacementCount()).isEqualTo(0);
    }

    @Test
    void replaceNormalAnnotation() {
        String source = """
                import javax.persistence.Table;

                @Table(name = "orders")
                public class Order {
                    private String id;
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@javax.persistence.Table",
                "@jakarta.persistence.Table(name = \"orders\")");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@Table(name = \"orders\")");
        assertThat(result.source()).contains("import jakarta.persistence.Table;");
        assertThat(result.source()).doesNotContain("import javax.persistence.Table;");
    }

    @Test
    void replaceSpringBootRequestMappingWithJakartaRS() {
        String source = """
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestMethod;

                public class GreetingController {
                    @RequestMapping(value = "/hello", method = RequestMethod.GET)
                    public String hello() {
                        return "Hello";
                    }

                    @RequestMapping(value = "/bye", method = RequestMethod.POST)
                    public String bye() {
                        return "Bye";
                    }
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@org.springframework.web.bind.annotation.RequestMapping",
                "@jakarta.ws.rs.Path(\"/hello\")");

        assertThat(result.replacementCount()).isEqualTo(2);
        assertThat(result.source()).contains("@Path(\"/hello\")");
        assertThat(result.source()).doesNotContain("@RequestMapping");
        assertThat(result.source()).contains("import jakarta.ws.rs.Path;");
        assertThat(result.source()).doesNotContain("import org.springframework.web.bind.annotation.RequestMapping;");
    }

    @Test
    void replaceSpringBootAutowiredWithJakartaInject() {
        String source = """
                import org.springframework.beans.factory.annotation.Autowired;

                public class OrderService {
                    @Autowired
                    private OrderRepository repository;

                    @Autowired
                    private NotificationService notificationService;
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@org.springframework.beans.factory.annotation.Autowired",
                "@jakarta.inject.Inject");

        assertThat(result.replacementCount()).isEqualTo(2);
        assertThat(result.source()).contains("@Inject");
        assertThat(result.source()).doesNotContain("@Autowired");
        assertThat(result.source()).contains("import jakarta.inject.Inject;");
        assertThat(result.source()).doesNotContain("import org.springframework.beans.factory.annotation.Autowired;");
    }

    @Test
    void replaceSpringBootComponentWithJakartaNamed() {
        String source = """
                import org.springframework.stereotype.Component;

                @Component
                public class EmailService {
                    public void send(String to, String body) {}
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source,
                "@org.springframework.stereotype.Component",
                "@jakarta.inject.Named");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@Named");
        assertThat(result.source()).doesNotContain("@Component");
        assertThat(result.source()).contains("import jakarta.inject.Named;");
        assertThat(result.source()).doesNotContain("import org.springframework.stereotype.Component;");
    }

    @Test
    void replaceClassLevelAnnotation() {
        String source = """
                @Deprecated
                public class LegacyService {
                    public void run() {}
                }
                """;

        ReplaceAnnotation.ReplacementResult result = ReplaceAnnotation.replace(source, "@Deprecated", "@Override");

        assertThat(result.replacementCount()).isEqualTo(1);
        assertThat(result.source()).contains("@Override");
        assertThat(result.source()).doesNotContain("@Deprecated");
    }
}