package dev.snowdrop.mtool.tests.project;

import dev.snowdrop.mtool.project.ApplicationConfig;
import dev.snowdrop.mtool.project.ProjectFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigTest {

    @TempDir
    Path project;

    @Test
    @DisplayName("Reads properties and flattens YAML to dotted keys")
    void readsBothFormats() throws IOException {
        write("application.properties", "quarkus.http.port=8081\n");
        write("application.yaml", "quarkus:\n  freemarker:\n    base-path: views\n    includes:\n      - '**/*.ftl'\n");

        ApplicationConfig config = config();

        assertThat(config.exists()).isTrue();
        assertThat(config.get("quarkus.http.port")).hasValue("8081");
        assertThat(config.get("quarkus.freemarker.base-path")).hasValue("views");
        assertThat(config.get("quarkus.freemarker.includes")).hasValue("**/*.ftl");
        assertThat(config.get("quarkus.missing")).isEmpty();
    }

    @Test
    @DisplayName("application.properties wins over application.yaml, profile files are ignored by get()")
    void precedence() throws IOException {
        write("application.yaml", "quarkus:\n  freemarker:\n    base-path: from-yaml\n");
        write("application.properties", "quarkus.freemarker.base-path=from-properties\n");
        write("application-dev.properties", "quarkus.freemarker.includes=**\n");

        ApplicationConfig config = config();

        assertThat(config.get("quarkus.freemarker.base-path")).hasValue("from-properties");
        assertThat(config.get("quarkus.freemarker.includes")).isEmpty();
    }

    @Test
    @DisplayName("filesDeclaring finds keys in every profile, not values or comments mentioning them")
    void filesDeclaring() throws IOException {
        write("application.properties", "# spring.freemarker.cache was removed\napp.note=see spring.freemarker docs\n");
        write("application-dev.yml", "spring:\n  freemarker:\n    cache: false\n");
        write("application-prod.yml", "spring:\n  datasource:\n    url: x\n---\nspring:\n  freemarker:\n    suffix: .ftl\n");

        ApplicationConfig config = config();

        assertThat(config.filesDeclaring("spring.freemarker"))
                .extracting(p -> p.getFileName().toString())
                .containsExactlyInAnyOrder("application-dev.yml", "application-prod.yml");
    }

    @Test
    @DisplayName("Only the configuration files of src/main/resources are read; invalid YAML is skipped")
    void scopeAndInvalidFiles() throws IOException {
        write("other.properties", "quarkus.a=1\n");
        Files.createDirectories(project.resolve("src/main/resources/config"));
        Files.writeString(project.resolve("src/main/resources/config/application.properties"), "quarkus.b=2\n");
        write("application.yml", "quarkus: [unclosed\n");

        ApplicationConfig config = config();

        assertThat(config.get("quarkus.a")).isEmpty();
        assertThat(config.get("quarkus.b")).isEmpty();
        assertThat(config.exists()).isTrue();
    }

    @Test
    @DisplayName("A project without configuration file")
    void noConfiguration() {
        assertThat(config().exists()).isFalse();
    }

    private ApplicationConfig config() {
        return ProjectFiles.scan(project).applicationConfig();
    }

    private void write(String name, String content) throws IOException {
        Path file = project.resolve("src/main/resources").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
