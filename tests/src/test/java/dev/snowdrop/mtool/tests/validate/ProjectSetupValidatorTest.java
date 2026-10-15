package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.validate.ProjectSetupValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectSetupValidatorTest {

    @TempDir
    Path tempDir;

    private final ProjectSetupValidator validator = new ProjectSetupValidator(true);

    @Test
    @DisplayName("Empty directory should fail most checks")
    void emptyProject() {
        ValidationReport report = validator.validate(tempDir);

        assertThat(report.failed()).isGreaterThanOrEqualTo(5);
        assertResult(report, "pom.xml exists", Status.FAILED);
        assertResult(report, "application.properties exists", Status.FAILED);
    }

    @Test
    @DisplayName("Should pass pom.xml exists when pom.xml is present")
    void pomExists() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), minimalPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "pom.xml exists", Status.PASSED);
    }

    @Test
    @DisplayName("Should pass application.properties check")
    void applicationPropertiesExists() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "quarkus.http.port=8080");

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "application.properties exists", Status.PASSED);
    }

    @Test
    @DisplayName("Should detect @SpringBootApplication annotation")
    void detectsSpringBootAnnotation() throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Files.writeString(javaDir.resolve("App.java"),
                "package com.example;\n@SpringBootApplication\npublic class App {}");

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring Boot files", Status.FAILED);
        assertEvidence(report, "No Spring Boot files", "@SpringBootApplication");
    }

    @Test
    @DisplayName("Should detect @SpringBootTest annotation")
    void detectsSpringBootTestAnnotation() throws IOException {
        Path testDir = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(testDir);
        Files.writeString(testDir.resolve("AppTest.java"),
                "package com.example;\n@SpringBootTest\npublic class AppTest {}");

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring Boot files", Status.FAILED);
        assertEvidence(report, "No Spring Boot files", "@SpringBootTest");
    }

    @Test
    @DisplayName("Should pass when no Spring Boot annotations exist")
    void noSpringAnnotations() throws IOException {
        Path javaDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaDir);
        Files.writeString(javaDir.resolve("App.java"),
                "package com.example;\npublic class App {}");

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring Boot files", Status.PASSED);
    }

    @Test
    @DisplayName("Should exclude target directory when scanning for annotations")
    void excludesTargetDirectory() throws IOException {
        Path targetDir = tempDir.resolve("target/classes/com/example");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("App.java"),
                "package com.example;\n@SpringBootApplication\npublic class App {}");

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring Boot files", Status.PASSED);
    }

    @Test
    @DisplayName("Should detect Spring dependencies in pom.xml")
    void detectsSpringDependencies() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), pomWithSpringDeps());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring dependencies", Status.FAILED);
        assertEvidence(report, "No Spring dependencies", "org.springframework.boot");
    }

    @Test
    @DisplayName("Should pass when no Spring dependencies in pom.xml")
    void noSpringDependencies() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "No Spring dependencies", Status.PASSED);
    }

    @Test
    @DisplayName("Should detect quarkus-maven-plugin")
    void detectsQuarkusPlugin() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "quarkus-maven-plugin", Status.PASSED);
    }

    @Test
    @DisplayName("Should fail when quarkus-maven-plugin is missing")
    void missingQuarkusPlugin() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), minimalPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "quarkus-maven-plugin", Status.FAILED);
    }

    @Test
    @DisplayName("Should detect quarkus-bom in dependencyManagement")
    void detectsQuarkusBom() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "quarkus-bom", Status.PASSED);
        assertEvidence(report, "quarkus-bom", "3.15.0");
    }

    @Test
    @DisplayName("Should resolve quarkus-bom via Maven property")
    void resolvesQuarkusBomViaProperty() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPomWithProperties());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "quarkus-bom", Status.PASSED);
        assertEvidence(report, "quarkus-bom", "3.15.0");
    }

    @Test
    @DisplayName("Should check standard directories")
    void checksStandardDirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        // src/test/java and src/test/resources missing

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "src/main/java exists", Status.PASSED);
        assertResult(report, "src/main/resources exists", Status.PASSED);
        assertResult(report, "src/test/java exists", Status.FAILED);
        assertResult(report, "src/test/resources exists", Status.FAILED);
    }

    @Test
    @DisplayName("Should verify POM coordinates")
    void checksPomCoordinates() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "POM coordinates", Status.PASSED);
    }

    @Test
    @DisplayName("Should fail POM coordinates when version is missing")
    void failsPomCoordinatesWithoutVersion() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), pomWithoutVersion());

        ValidationReport report = validator.validate(tempDir);

        assertResult(report, "POM coordinates", Status.FAILED);
        assertEvidence(report, "POM coordinates", "version");
    }

    @Test
    @DisplayName("Overall status should be PASS when all checks pass")
    void overallStatusPass() throws IOException {
        setupFullQuarkusProject();

        ValidationReport report = validator.validate(tempDir);

        assertThat(report.failed()).isZero();
        assertThat(report.overallStatus()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("Overall status should be PARTIAL when some checks fail")
    void overallStatusPartial() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createDirectories(tempDir.resolve("src/test/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "");
        // Missing: no Spring annotation check passes but more pass than fail

        ValidationReport report = validator.validate(tempDir);

        assertThat(report.overallStatus()).isEqualTo("PASS");
    }

    // --- helpers ---

    private void setupFullQuarkusProject() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), quarkusPom());
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createDirectories(tempDir.resolve("src/test/resources"));
        Files.writeString(tempDir.resolve("src/main/resources/application.properties"), "quarkus.http.port=8080");
        Files.writeString(tempDir.resolve("src/main/java/com/example/App.java"),
                "package com.example;\npublic class App {}");
    }

    private void assertResult(ValidationReport report, String ruleName, Status expected) {
        ValidationResult result = findResult(report, ruleName);
        assertThat(result)
                .as("Expected result for rule '%s'", ruleName)
                .isNotNull();
        assertThat(result.status())
                .as("Status for rule '%s'", ruleName)
                .isEqualTo(expected);
    }

    private void assertEvidence(ValidationReport report, String ruleName, String expectedSubstring) {
        ValidationResult result = findResult(report, ruleName);
        assertThat(result).isNotNull();
        assertThat(result.evidence())
                .as("Evidence for rule '%s'", ruleName)
                .contains(expectedSubstring);
    }

    private ValidationResult findResult(ValidationReport report, String ruleName) {
        return report.getResults().stream()
                .filter(r -> r.rule().name().equals(ruleName))
                .findFirst()
                .orElse(null);
    }

    private String minimalPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>
                </project>
                """;
    }

    private String pomWithoutVersion() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                </project>
                """;
    }

    private String pomWithSpringDeps() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;
    }

    private String quarkusPom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-quarkus-app</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>3.15.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-rest</artifactId>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>3.15.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
    }

    private String quarkusPomWithProperties() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-quarkus-app</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
                        <quarkus.platform.version>3.15.0</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus</groupId>
                                <artifactId>${quarkus.platform.artifact-id}</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus.platform.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
    }
}