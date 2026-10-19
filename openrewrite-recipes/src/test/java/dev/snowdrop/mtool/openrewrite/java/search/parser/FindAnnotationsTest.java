package dev.snowdrop.mtool.openrewrite.java.search.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindAnnotationsTest {

    @Test
    void findMarkerAnnotation() {
        String source = """
                public class HomeCinema {
                    @Deprecated
                    public void display() {
                        System.out.println("Deprecated display()");
                    }
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "@java.lang.Deprecated");

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().annotationName()).isEqualTo("Deprecated");
        assertThat(matches.getFirst().enclosingClass()).isEqualTo("HomeCinema");
        assertThat(matches.getFirst().line()).isEqualTo(2);
    }

    @Test
    void findAndMarkAnnotation() {
        String source = """
                public class HomeCinema {
                    @Deprecated
                    public void display() {
                        System.out.println("Deprecated display()");
                    }
                }
                """;

        String marked = FindAnnotations.findAndMark(source, "@java.lang.Deprecated");

        assertThat(marked).isEqualTo("""
                public class HomeCinema {
                    /*~~>*/@Deprecated
                    public void display() {
                        System.out.println("Deprecated display()");
                    }
                }
                """);
    }

    @Test
    void findAnnotationBySimpleName() {
        String source = """
                public class OrderService {
                    @Override
                    public String toString() {
                        return "OrderService";
                    }
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "Override");

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().enclosingClass()).isEqualTo("OrderService");
    }

    @Test
    void findMultipleAnnotations() {
        String source = """
                public class Controller {
                    @Deprecated
                    @Override
                    public String handle() {
                        return "";
                    }
                    @Deprecated
                    public void legacy() {}
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "Deprecated");

        assertThat(matches).hasSize(2);
        assertThat(matches).allSatisfy(m -> assertThat(m.enclosingClass()).isEqualTo("Controller"));
    }

    @Test
    void findAndMarkMultipleAnnotations() {
        String source = """
                public class Controller {
                    @Deprecated
                    @Override
                    public String handle() {
                        return "";
                    }
                    @Deprecated
                    public void legacy() {}
                }
                """;

        String marked = FindAnnotations.findAndMark(source, "Deprecated");

        assertThat(marked).isEqualTo("""
                public class Controller {
                    /*~~>*/@Deprecated
                    @Override
                    public String handle() {
                        return "";
                    }
                    /*~~>*/@Deprecated
                    public void legacy() {}
                }
                """);
    }

    @Test
    void findSingleMemberAnnotation() {
        String source = """
                public class Calc {
                    @SuppressWarnings("unchecked")
                    public void compute() {}
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "@java.lang.SuppressWarnings");

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().annotationName()).isEqualTo("SuppressWarnings");
    }

    @Test
    void findNormalAnnotation() {
        String source = """
                import java.lang.annotation.*;

                public class Config {
                    @Retention(RetentionPolicy.RUNTIME)
                    @Target(ElementType.METHOD)
                    public @interface MyAnnotation {
                        String value();
                        int count();
                    }
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "Retention");

        assertThat(matches).hasSize(1);
    }

    @Test
    void noMatchReturnsEmptyList() {
        String source = """
                public class Plain {
                    public void doSomething() {}
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "Deprecated");

        assertThat(matches).isEmpty();
    }

    @Test
    void findAndMarkNoMatchReturnsOriginal() {
        String source = """
                public class Plain {
                    public void doSomething() {}
                }
                """;

        String marked = FindAnnotations.findAndMark(source, "Deprecated");

        assertThat(marked).isEqualTo(source);
    }

    @Test
    void findClassLevelAnnotation() {
        String source = """
                @Deprecated
                public class LegacyService {
                    public void run() {}
                }
                """;

        List<FindAnnotations.AnnotationMatch> matches = FindAnnotations.find(source, "Deprecated");

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().enclosingClass()).isEqualTo("LegacyService");
    }
}