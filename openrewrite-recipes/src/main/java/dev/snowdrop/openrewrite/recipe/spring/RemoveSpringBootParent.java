package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSpringBootParent extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove Spring Boot 3.x parent POM";
    }

    @Override
    public String getDescription() {
        return "Removes the Spring Boot 3.x starter parent POM from Maven projects.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {

            @Override
            public  Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isSpringBootParent(tag)) {
                    // Remove the parent tag entirely
                    return null;
                }
                return super.visitTag(tag, ctx);
            }

            private boolean isSpringBootParent(Xml.Tag tag) {
                if (!"parent".equals(tag.getName())) {
                    return false;
                }

                // Check if this is a Spring Boot parent by examining its children
                String groupId = tag.getChildValue("groupId").orElse("");
                String artifactId = tag.getChildValue("artifactId").orElse("");
                String version = tag.getChildValue("version").orElse("");

                // Only process Spring Boot 3.x parent POMs
                return "org.springframework.boot".equals(groupId) &&
                    "spring-boot-starter-parent".equals(artifactId) &&
                    version.startsWith("3.");
            }
        };
    }
}
