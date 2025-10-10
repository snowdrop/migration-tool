package dev.snowdrop.openrewrite.recipe.query;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
  type: specs.openrewrite.org/v1beta/recipe
  name: com.myorg.FindTodoListClass
  recipeList:
    - dev.snowdrop.openrewrite.recipe.query.QueryRecipe:
        query: "FIND CLASS IN JAVA WHERE name='TodoList'"
        query: "FIND DEPENDENCY IN POM WHERE artifactId='quarkus-core'"
        query: "FIND DEPENDENCY IN POM WHERE (artifactId='quarkus-core' AND version='3.16.2') OR (artifactId='quarkus-rest' AND version='3.16.2')"
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class QueryRecipe extends Recipe {

    @Option(displayName = "Query", description = "The unified query to execute.",
        example = "FIND CLASS IN JAVA WHERE name='UserService'")
    String query;

    @Override
    public String getDisplayName() {
        return "Unified Query Recipe";
    }

    @Override
    public String getDescription() {
        return "A single recipe to query different file types using a simple language.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        Pattern queryPattern = Pattern.compile("FIND\\s+(\\w+)\\s+IN\\s+([\\w.]+)\\s+WHERE\\s+(.*)");
        Matcher matcher = queryPattern.matcher(query);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query syntax: " + query);
        }

        String elementType = matcher.group(1).toUpperCase();
        String fileType = matcher.group(2).toUpperCase();
        Condition condition = QueryParser.parse(matcher.group(3));

        switch (fileType) {
            case "POM":
                return new XmlQueryVisitor(elementType, condition);
            case "JAVA":
                return new JavaQueryVisitor(elementType, condition);
            default:
                return TreeVisitor.noop();
        }
    }

    // --- Inner Visitor for Java ---
    private static class JavaQueryVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String elementType;
        private final Condition rootCondition;

        public JavaQueryVisitor(String elementType, Condition rootCondition) {
            this.elementType = elementType;
            this.rootCondition = rootCondition;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
            if ("CLASS".equals(elementType)) {
                Map<String, String> properties = new HashMap<>();
                properties.put("name", c.getSimpleName());
                // You could add more properties like "package", "modifiers", etc.

                if (rootCondition.evaluate(properties)) {
                    return SearchResult.found(c, "found class by query");
                }
            }
            return c;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if ("METHOD".equals(elementType)) {
                Map<String, String> properties = new HashMap<>();
                properties.put("name", m.getSimpleName());
                properties.put("modifiers", m.getModifiers().stream()
                    .map(mod -> mod.getType().name().toLowerCase())
                    .collect(Collectors.joining(" ")));
                // You could add "returnType", "parameterCount", etc.

                if (rootCondition.evaluate(properties)) {
                    return SearchResult.found(m, "found method by query");
                }
            }
            return m;
        }
    }

    // --- Inner Visitor for XML ---
    private static class XmlQueryVisitor extends XmlIsoVisitor<ExecutionContext> {
        private final String elementType;
        private final Condition rootCondition;

        public XmlQueryVisitor(String elementType, Condition rootCondition) {
            this.elementType = elementType;
            this.rootCondition = rootCondition;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if ("DEPENDENCY".equals(elementType) && "dependency".equals(t.getName())) {
                Map<String, String> properties = new HashMap<>();
                t.getChildValue("groupId").ifPresent(v -> properties.put("groupId", v));
                t.getChildValue("artifactId").ifPresent(v -> properties.put("artifactId", v));
                t.getChildValue("version").ifPresent(v -> properties.put("version", v));

                if (rootCondition.evaluate(properties)) {
                    return SearchResult.found(t, "found dependency by query");
                }
            }
            return t;
        }
    }
}