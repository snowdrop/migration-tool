package dev.snowdrop.openrewrite.java.search;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.java.Assertions.java;

public class FindAnnotationTest implements RewriteTest {
    @Test
    public void testFindAnnotation() {

        FindAnnotations findAnnotation = new FindAnnotations(false);
        findAnnotation.matchId = "match-deprecated-001";
        findAnnotation.pattern = "@java.lang.Deprecated";
        // TODO: Check with openrewrite why we got 2 results in the datatable !!
        rewriteRun(
            spec -> spec.dataTableAsCsv(MatchingReport.class,
                        """
                        matchId,type,symbol,pattern,sourceFilePath,className
                        match-deprecated-001,JAVA,ANNOTATION,@java.lang.Deprecated,HomeCinema.java,HomeCinema
                        match-deprecated-001,JAVA,ANNOTATION,@java.lang.Deprecated,HomeCinema.java,HomeCinema
                        """
                    ).recipe(findAnnotation),
        java(
            """
            public class HomeCinema {
                @Deprecated
                public void Display() {
                    System.out.println("Deprecated display()");
                }
            }
            """,
            """
            public class HomeCinema {
                /*~~>*/@Deprecated
                public void Display() {
                    System.out.println("Deprecated display()");
                }
            }
            """
        ));
    }
}
