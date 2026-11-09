package dev.snowdrop.mtool.project.build;

import dev.snowdrop.mtool.project.TextFiles;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Declared dependencies of a Maven or Gradle project.
 *
 * For Maven, only the {@code <dependency>} elements of {@code project/dependencies} and
 * {@code project/profiles/profile/dependencies} are considered: entries of {@code dependencyManagement},
 * plugin dependencies, exclusions and XML comments are ignored. The poms of the sub-modules found under
 * the project directory are included.
 */
public final class BuildFile {

    private static final Logger logger = Logger.getLogger(BuildFile.class);

    public enum BuildSystem {
        MAVEN,
        GRADLE;

        /**
         * Build system of the project, from the build file at its root. Maven wins when the project has both
         * a pom.xml and a Gradle build file. Returns null when there is no build file.
         */
        public static BuildSystem detect(Path projectDir) {
            if (Files.exists(projectDir.resolve("pom.xml"))) {
                return MAVEN;
            }
            if (GRADLE_FILES.stream().anyMatch(f -> Files.exists(projectDir.resolve(f)))) {
                return GRADLE;
            }
            return null;
        }
    }

    private static final List<String> GRADLE_FILES = List.of("build.gradle", "build.gradle.kts");

    private final BuildSystem buildSystem;
    private final Set<String> mavenCoordinates;
    private final String gradleContent;

    private BuildFile(BuildSystem buildSystem, Set<String> mavenCoordinates, String gradleContent) {
        this.buildSystem = buildSystem;
        this.mavenCoordinates = mavenCoordinates;
        this.gradleContent = gradleContent;
    }

    /**
     * @param projectDir project root
     * @param moduleBuildFiles build files of the sub-modules (pom.xml or build.gradle[.kts]), may be empty
     */
    public static BuildFile load(Path projectDir, List<Path> moduleBuildFiles) {
        BuildSystem buildSystem = BuildSystem.detect(projectDir);
        if (buildSystem == BuildSystem.MAVEN) {
            Set<String> coordinates = new HashSet<>();
            Stream.concat(Stream.of(projectDir.resolve("pom.xml")), moduleBuildFiles.stream())
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .distinct()
                    .forEach(pom -> coordinates.addAll(mavenDependencies(pom)));
            return new BuildFile(BuildSystem.MAVEN, coordinates, "");
        }
        if (buildSystem == BuildSystem.GRADLE) {
            StringBuilder content = new StringBuilder();
            Stream.concat(GRADLE_FILES.stream().map(projectDir::resolve), moduleBuildFiles.stream())
                    .filter(p -> GRADLE_FILES.contains(p.getFileName().toString()))
                    .filter(Files::exists)
                    .distinct()
                    .forEach(f -> content.append(TextFiles.read(f)).append('\n'));
            return new BuildFile(BuildSystem.GRADLE, Set.of(), content.toString());
        }
        return new BuildFile(null, Set.of(), "");
    }

    public boolean exists() {
        return buildSystem != null;
    }

    public BuildSystem buildSystem() {
        return buildSystem;
    }

    public boolean hasDependency(Dependency dependency) {
        String groupId = dependency.groupId();
        if (buildSystem == BuildSystem.MAVEN) {
            return dependency.anyArtifact()
                    ? mavenCoordinates.stream().anyMatch(c -> c.startsWith(groupId + ":"))
                    : mavenCoordinates.contains(groupId + ":" + dependency.artifactId());
        }
        if (buildSystem == BuildSystem.GRADLE) {
            // "group:artifact" followed by a version separator or a closing quote, so that
            // io.quarkus:quarkus-qute does not match io.quarkus:quarkus-qute-web
            String artifact = dependency.anyArtifact() ? "[\\w.-]+" : Pattern.quote(dependency.artifactId());
            return Pattern.compile("[\"']" + Pattern.quote(groupId) + ":" + artifact + "(?=[:\"'@])")
                    .matcher(gradleContent).find();
        }
        return false;
    }

    public boolean hasAnyDependency(List<Dependency> dependencies) {
        return dependencies.stream().anyMatch(this::hasDependency);
    }

    private static Set<String> mavenDependencies(Path pom) {
        Set<String> coordinates = new HashSet<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(pom.toFile());
            NodeList dependencies = doc.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dependency = (Element) dependencies.item(i);
                if (isDeclaredDependency(dependency)) {
                    String groupId = childText(dependency, "groupId");
                    String artifactId = childText(dependency, "artifactId");
                    if (groupId != null && artifactId != null) {
                        coordinates.add(groupId + ":" + artifactId);
                    }
                }
            }
        } catch (Exception e) {
            logger.warnf("Could not parse %s: %s", pom, e.getMessage());
        }
        return coordinates;
    }

    /** True for project/dependencies/dependency and project/profiles/profile/dependencies/dependency. */
    private static boolean isDeclaredDependency(Element dependency) {
        Node dependencies = dependency.getParentNode();
        if (dependencies == null || !"dependencies".equals(dependencies.getNodeName())) {
            return false;
        }
        Node owner = dependencies.getParentNode();
        return owner != null && ("project".equals(owner.getNodeName()) || "profile".equals(owner.getNodeName()));
    }

    private static String childText(Element parent, String tagName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && tagName.equals(element.getTagName())) {
                return element.getTextContent().trim();
            }
        }
        return null;
    }
}
