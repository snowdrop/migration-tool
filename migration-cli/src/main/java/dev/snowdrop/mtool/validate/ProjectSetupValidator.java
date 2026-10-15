package dev.snowdrop.mtool.validate;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ProjectSetupValidator implements Validator {

    private static final Logger logger = Logger.getLogger(ProjectSetupValidator.class);

    private boolean skipCompile;

    public ProjectSetupValidator() {
    }

    public ProjectSetupValidator(boolean skipCompile) {
        this.skipCompile = skipCompile;
    }

    @Override
    public String name() {
        return "project-setup";
    }

    @Override
    public ValidationReport validate(Path projectPath) {
        ValidationReport report = new ValidationReport(name());

        checkPomExists(report, projectPath);
        checkApplicationProperties(report, projectPath);
        checkNoSpringBootAnnotation(report, projectPath);
        checkNoSpringDependencies(report, projectPath);
        checkPomCoordinates(report, projectPath);
        checkQuarkusMavenPlugin(report, projectPath);
        checkQuarkusBom(report, projectPath);
        checkStandardDirectories(report, projectPath);
        if (!skipCompile) {
            checkMvnCompile(report, projectPath);
        }

        return report;
    }

    private void checkPomExists(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("pom.xml exists", "Project must have a pom.xml file");
        Path pomPath = projectPath.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.PASSED, "File exists: pom.xml"));
        } else {
            report.add(new ValidationResult(rule, Status.FAILED, "File not found: pom.xml"));
        }
    }

    private void checkApplicationProperties(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("application.properties exists",
                "Project must have application.properties");
        Path propsPath = projectPath.resolve("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            report.add(new ValidationResult(rule, Status.PASSED, "File exists: src/main/resources/application.properties"));
        } else {
            report.add(new ValidationResult(rule, Status.FAILED,
                    "File not found: src/main/resources/application.properties"));
        }
    }

    private void checkNoSpringBootAnnotation(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("No Spring Boot files",
                "No Spring Boot annotations should remain");
        Set<String> excludedDirs = Set.of("target", ".m2", ".git");
        Set<String> springAnnotations = Set.of(
                "@SpringBootApplication",
                "@EnableAutoConfiguration",
                "@SpringBootTest");

        try (Stream<Path> javaFiles = Files.walk(projectPath)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> excludedDirs.stream().noneMatch(d -> p.toString().contains("/" + d + "/")))) {

            List<String> hits = new ArrayList<>();
            javaFiles.forEach(file -> {
                try {
                    String content = Files.readString(file);
                    for (String annotation : springAnnotations) {
                        if (content.contains(annotation)) {
                            hits.add(projectPath.relativize(file) + " (" + annotation + ")");
                            break;
                        }
                    }
                } catch (IOException e) {
                    // skip unreadable files
                }
            });

            if (hits.isEmpty()) {
                report.add(new ValidationResult(rule, Status.PASSED, "No Spring Boot annotations found"));
            } else {
                String sample = String.join("; ", hits.subList(0, Math.min(3, hits.size())));
                String extra = hits.size() > 3 ? " (and " + (hits.size() - 3) + " more)" : "";
                report.add(new ValidationResult(rule, Status.FAILED,
                        "Spring Boot annotations found: " + sample + extra));
            }
        } catch (IOException e) {
            report.add(new ValidationResult(rule, Status.WARNING, "Could not scan Java sources: " + e.getMessage()));
        }
    }

    private void checkNoSpringDependencies(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("No Spring dependencies",
                "pom.xml should not contain Spring Boot starter dependencies");
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.WARNING, "Cannot check — pom.xml not found"));
            return;
        }

        try {
            Document doc = parsePom(pomPath);
            NodeList dependencies = doc.getElementsByTagName("dependency");
            StringBuilder springDeps = new StringBuilder();
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                String groupId = getElementText(dep, "groupId");
                String artifactId = getElementText(dep, "artifactId");
                if (groupId != null && groupId.startsWith("org.springframework")) {
                    if (!springDeps.isEmpty()) {
                        springDeps.append(", ");
                    }
                    springDeps.append(groupId).append(":").append(artifactId);
                }
            }
            if (springDeps.isEmpty()) {
                report.add(new ValidationResult(rule, Status.PASSED, "No Spring dependencies found in pom.xml"));
            } else {
                report.add(new ValidationResult(rule, Status.FAILED,
                        "Spring dependencies found: " + springDeps));
            }
        } catch (Exception e) {
            report.add(new ValidationResult(rule, Status.WARNING, "Could not parse pom.xml: " + e.getMessage()));
        }
    }

    private void checkPomCoordinates(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("POM coordinates",
                "pom.xml must have groupId, artifactId and version");
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.FAILED, "Cannot check — pom.xml not found"));
            return;
        }

        try {
            Document doc = parsePom(pomPath);
            Element root = doc.getDocumentElement();
            String groupId = getDirectChildText(root, "groupId");
            String artifactId = getDirectChildText(root, "artifactId");
            String version = getDirectChildText(root, "version");

            List<String> missing = new ArrayList<>();
            if (groupId == null)
                missing.add("groupId");
            if (artifactId == null)
                missing.add("artifactId");
            if (version == null)
                missing.add("version");

            if (missing.isEmpty()) {
                report.add(new ValidationResult(rule, Status.PASSED, "pom.xml has groupId, artifactId, version"));
            } else {
                report.add(new ValidationResult(rule, Status.FAILED,
                        "Missing POM coordinates: " + String.join(", ", missing)));
            }
        } catch (Exception e) {
            report.add(new ValidationResult(rule, Status.WARNING, "Could not parse pom.xml: " + e.getMessage()));
        }
    }

    private void checkQuarkusMavenPlugin(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("quarkus-maven-plugin",
                "quarkus-maven-plugin must be present in pom.xml");
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.FAILED, "Cannot check — pom.xml not found"));
            return;
        }

        try {
            Document doc = parsePom(pomPath);
            Map<String, String> properties = extractProperties(doc);
            NodeList plugins = doc.getElementsByTagName("plugin");
            for (int i = 0; i < plugins.getLength(); i++) {
                Element plugin = (Element) plugins.item(i);
                String artifactId = resolveProperty(getElementText(plugin, "artifactId"), properties);
                if ("quarkus-maven-plugin".equals(artifactId)) {
                    report.add(new ValidationResult(rule, Status.PASSED,
                            "quarkus-maven-plugin is present in pom.xml"));
                    return;
                }
            }
            report.add(new ValidationResult(rule, Status.FAILED, "quarkus-maven-plugin not found in pom.xml"));
        } catch (Exception e) {
            report.add(new ValidationResult(rule, Status.WARNING, "Could not parse pom.xml: " + e.getMessage()));
        }
    }

    private void checkQuarkusBom(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("quarkus-bom",
                "quarkus-bom must be in dependencyManagement");
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.FAILED, "Cannot check — pom.xml not found"));
            return;
        }

        try {
            Document doc = parsePom(pomPath);
            Map<String, String> properties = extractProperties(doc);
            NodeList depMgmtList = doc.getElementsByTagName("dependencyManagement");
            for (int i = 0; i < depMgmtList.getLength(); i++) {
                Element depMgmt = (Element) depMgmtList.item(i);
                NodeList deps = depMgmt.getElementsByTagName("dependency");
                for (int j = 0; j < deps.getLength(); j++) {
                    Element dep = (Element) deps.item(j);
                    String artifactId = resolveProperty(getElementText(dep, "artifactId"), properties);
                    if ("quarkus-bom".equals(artifactId)) {
                        String version = resolveProperty(getElementText(dep, "version"), properties);
                        String versionInfo = version != null ? " (version: " + version + ")" : "";
                        report.add(new ValidationResult(rule, Status.PASSED,
                                "quarkus-bom found in dependencyManagement" + versionInfo));
                        return;
                    }
                }
            }
            report.add(new ValidationResult(rule, Status.FAILED,
                    "quarkus-bom not found in dependencyManagement"));
        } catch (Exception e) {
            report.add(new ValidationResult(rule, Status.WARNING, "Could not parse pom.xml: " + e.getMessage()));
        }
    }

    private void checkStandardDirectories(ValidationReport report, Path projectPath) {
        String[] dirs = {
                "src/main/java",
                "src/main/resources",
                "src/test/java",
                "src/test/resources"
        };
        for (String dir : dirs) {
            ValidationRule rule = new ValidationRule(dir + " exists",
                    "Standard directory " + dir + " must exist");
            Path dirPath = projectPath.resolve(dir);
            if (Files.isDirectory(dirPath)) {
                report.add(new ValidationResult(rule, Status.PASSED, "Directory exists: " + dir));
            } else {
                report.add(new ValidationResult(rule, Status.FAILED, "Directory not found: " + dir));
            }
        }
    }

    private void checkMvnCompile(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("mvn compile", "Project must compile successfully");
        Path pomPath = projectPath.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            report.add(new ValidationResult(rule, Status.FAILED, "Cannot compile — pom.xml not found"));
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "-B", "--no-transfer-progress")
                    .directory(projectPath.toFile())
                    .redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                report.add(new ValidationResult(rule, Status.FAILED,
                        "mvn compile timed out after 180 seconds"));
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                report.add(new ValidationResult(rule, Status.PASSED, "mvn compile — BUILD SUCCESS"));
            } else {
                String failureHint = extractBuildFailure(output.toString());
                report.add(new ValidationResult(rule, Status.FAILED,
                        "mvn compile — BUILD FAILURE" + (failureHint.isEmpty() ? "" : ": " + failureHint)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            report.add(new ValidationResult(rule, Status.FAILED, "mvn compile interrupted"));
        } catch (IOException e) {
            report.add(new ValidationResult(rule, Status.WARNING,
                    "Could not run mvn compile: " + e.getMessage()));
        }
    }

    private String extractBuildFailure(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("[ERROR]") && !line.contains("BUILD FAILURE")) {
                return line.replaceFirst(".*\\[ERROR]\\s*", "").trim();
            }
        }
        return "";
    }

    private Document parsePom(Path pomPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(pomPath.toFile());
    }

    private Map<String, String> extractProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();
        NodeList propsList = doc.getElementsByTagName("properties");
        if (propsList.getLength() > 0) {
            Element propsElement = (Element) propsList.item(0);
            NodeList children = propsElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element child) {
                    properties.put(child.getTagName(), child.getTextContent().trim());
                }
            }
        }
        return properties;
    }

    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        String resolved = value;
        while (resolved.contains("${")) {
            int start = resolved.indexOf("${");
            int end = resolved.indexOf("}", start);
            if (end < 0) {
                break;
            }
            String key = resolved.substring(start + 2, end);
            String replacement = properties.getOrDefault(key, resolved.substring(start, end + 1));
            resolved = resolved.substring(0, start) + replacement + resolved.substring(end + 1);
        }
        return resolved;
    }

    private String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(tagName)) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
}