package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMethodBodyContent extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace method body content";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Replace method body content.";
    }

    @Option(displayName = "Name of the method to search", description = "Name of the method where we will remove the parameters")
    String methodToSearch;

    @Option(displayName = "String replaced", description = "String replaced")
    String replacement;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodBodyMainVisitor();
    }

    private class MethodBodyMainVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            JavaTemplate t = JavaTemplate.builder(replacement)
                .build();
            if (methodToSearch.contains(method.getSimpleName())) {
                return t.apply(getCursor(), method.getCoordinates().replaceBody());
            }
            return super.visitMethodDeclaration(method, ctx);
        }
    }
}
