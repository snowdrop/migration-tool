package dev.snowdrop.openrewrite.recipe.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddQuarkusMavenPlugin extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add Quarkus Maven plugin";
    }

    @Override
    public String getDescription() {
        return "Adds the Quarkus Maven plugin using the same version as the quarkus-bom in dependency management.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Optional<String> quarkusVersion = getResolutionResult().getPom().getRequested().getDependencyManagement().stream()
                    .filter(dep -> "io.quarkus.platform".equals(dep.getGroupId()) && "quarkus-bom".equals(dep.getArtifactId()))
                    .map(ManagedDependency::getVersion)
                    .findFirst();
                //noinspection OptionalIsPresent
                if (!quarkusVersion.isPresent()) {
                    return document;
                }
                return (Xml.Document) new AddPlugin(
                    "io.quarkus.platform",
                    "quarkus-maven-plugin",
                    quarkusVersion.get(),
                    null,
                    null,
                    null,
                    null)
                    .getVisitor()
                    .visitNonNull(document, ctx);
            }
        };
    }
}