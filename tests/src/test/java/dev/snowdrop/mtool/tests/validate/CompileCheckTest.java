package dev.snowdrop.mtool.tests.validate;

import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.validate.CompileCheck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compile check shared by the validators. The build tools are replaced by fake wrapper scripts.
 */
class CompileCheckTest {

    @TempDir
    Path target;

    @Test
    @DisplayName("mvn compile check: a build that cannot be started fails")
    void compileCheckNotRunnableFails() throws IOException {
        write(target, "pom.xml", "<project/>");
        writeExecutable(target, "mvnw", "#!/nonexistent/interpreter\n");

        ValidationResult result = CompileCheck.run(new ValidationRule("mvn compile", "Project compiles"), target);

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.evidence()).startsWith("Could not run mvn compile");
    }

    @Test
    @DisplayName("Compile check: a Gradle project is compiled with gradle compileJava")
    void compileCheckGradle() throws IOException {
        write(target, "build.gradle", "dependencies {}\n");
        writeExecutable(target, "gradlew", "#!/bin/sh\necho \"$@\" > gradle-args.txt\nexit 0\n");

        ValidationResult result = CompileCheck.run("Project compiles", target);

        assertThat(result.rule().name()).isEqualTo("gradle compileJava");
        assertThat(result.status()).isEqualTo(Status.PASSED);
        assertThat(result.evidence()).isEqualTo("gradle compileJava — BUILD SUCCESS");
        assertThat(Files.readString(target.resolve("gradle-args.txt")).trim()).isEqualTo("compileJava");
    }

    @Test
    @DisplayName("Compile check: Maven wins when the project has both build files")
    void compileCheckPrefersMaven() throws IOException {
        write(target, "pom.xml", "<project/>");
        write(target, "build.gradle", "dependencies {}\n");
        writeExecutable(target, "mvnw", "#!/bin/sh\nexit 0\n");
        writeExecutable(target, "gradlew", "#!/bin/sh\nexit 1\n");

        ValidationResult result = CompileCheck.run("Project compiles", target);

        assertThat(result.rule().name()).isEqualTo("mvn compile");
        assertThat(result.status()).isEqualTo(Status.PASSED);
    }

    @Test
    @DisplayName("Compile check: a project without build file fails without running a build")
    void compileCheckWithoutBuildFile() {
        ValidationResult result = CompileCheck.run("Project compiles", target);

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.evidence()).contains("no build file found");
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static void writeExecutable(Path root, String relativePath, String content) throws IOException {
        write(root, relativePath, content);
        assertThat(root.resolve(relativePath).toFile().setExecutable(true)).isTrue();
    }
}
