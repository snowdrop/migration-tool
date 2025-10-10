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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
  type: specs.openrewrite.org/v1beta/recipe
  name: com.myorg.FindTodoListClass
  recipeList:
    - dev.snowdrop.openrewrite.recipe.query.QueryRecipe:
        query: "FIND CLASS IN JAVA WHERE name='TodoList'"

  type: specs.openrewrite.org/v1beta/recipe
  name: com.myorg.FindQuarkusCoreDependency
  recipeList:
    - dev.snowdrop.openrewrite.recipe.query.QueryRecipe:
      query: "FIND DEPENDENCY IN POM.XML WHERE artifactId='quarkus-core'"
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

    // A simple parser for the "key='value'" conditions
    private static Map<String, String> parseConditions(String conditionsStr) {
        Map<String, String> conditions = new HashMap<>();
        Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*'([^']*)'");
        Matcher matcher = pattern.matcher(conditionsStr);
        while (matcher.find()) {
            conditions.put(matcher.group(1), matcher.group(2));
        }
        return conditions;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // Regex to parse the query string
        Pattern queryPattern = Pattern.compile("FIND\\s+(\\w+)\\s+IN\\s+([\\w.]+)\\s+WHERE\\s+(.*)");
        Matcher matcher = queryPattern.matcher(query);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query syntax: " + query);
        }

        String elementType = matcher.group(1).toUpperCase();
        String fileType = matcher.group(2).toUpperCase();
        String conditionsStr = matcher.group(3);
        Map<String, String> conditions = parseConditions(conditionsStr);

        // The "dispatcher" logic
        switch (fileType) {
            case "JAVA":
                return new JavaQueryVisitor(elementType, conditions);
            case "POM.XML":
                return new XmlQueryVisitor(elementType, conditions);
            default:
                return TreeVisitor.noop();
        }
    }

    // --- Inner Visitor for Java ---
    private static class JavaQueryVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String elementType;
        private final Map<String, String> conditions;

        public JavaQueryVisitor(String elementType, Map<String, String> conditions) {
            this.elementType = elementType;
            this.conditions = conditions;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if ("CLASS".equals(elementType) && classDecl.getSimpleName().equals(conditions.get("name"))) {
                return SearchResult.found(classDecl, "found class by query");
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }

    // --- Inner Visitor for XML ---
    private static class XmlQueryVisitor extends XmlIsoVisitor<ExecutionContext> {
        private final String elementType;
        private final Map<String, String> conditions;

        public XmlQueryVisitor(String elementType, Map<String, String> conditions) {
            this.elementType = elementType;
            this.conditions = conditions;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if ("DEPENDENCY".equals(elementType) && "dependency".equals(tag.getName())) {
                Optional<String> artifactId = tag.getChildValue("artifactId");
                if (artifactId.isPresent() && artifactId.get().equals(conditions.get("artifactId"))) {
                    return SearchResult.found(tag, "found dependency by query");
                }
            }
            return super.visitTag(tag, ctx);
        }
    }
}