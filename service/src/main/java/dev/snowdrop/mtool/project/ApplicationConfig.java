package dev.snowdrop.mtool.project;

import org.jboss.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Application configuration of a project: the {@code application.properties} and {@code application.yaml}/
 * {@code .yml} files of {@code src/main/resources}, including the profile files ({@code application-dev.yml}).
 *
 * YAML files are flattened to dotted keys ({@code quarkus: freemarker: base-path: x} becomes
 * {@code quarkus.freemarker.base-path=x}) so that both formats are read the same way.
 */
public final class ApplicationConfig {

    private static final Logger logger = Logger.getLogger(ApplicationConfig.class);

    private static final Pattern CONFIG_FILE = Pattern.compile("application(-[\\w.-]+)?\\.(properties|ya?ml)");
    private static final String MAIN_PROPERTIES = "application.properties";

    /** Entries of each configuration file, by file. */
    private final Map<Path, Map<String, String>> files;

    private ApplicationConfig(Map<Path, Map<String, String>> files) {
        this.files = files;
    }

    static ApplicationConfig load(ProjectFiles project) {
        Map<Path, Map<String, String>> files = new LinkedHashMap<>();
        Path resources = project.resolve(ProjectFiles.RESOURCES_DIR);
        for (Path file : project.find(ProjectFiles.RESOURCES_DIR, ".properties", ".yml", ".yaml")) {
            if (resources.equals(file.getParent()) && CONFIG_FILE.matcher(file.getFileName().toString()).matches()) {
                files.put(file, parse(file, project.read(file)));
            }
        }
        return new ApplicationConfig(files);
    }

    /** True when the project has at least one configuration file. */
    public boolean exists() {
        return !files.isEmpty();
    }

    /**
     * Value of a key in the default profile: {@code application.properties} wins over
     * {@code application.yaml}/{@code .yml}, as in Quarkus. Profile files are ignored.
     */
    public Optional<String> get(String key) {
        Optional<String> yaml = Optional.empty();
        for (Map.Entry<Path, Map<String, String>> file : files.entrySet()) {
            String name = file.getKey().getFileName().toString();
            String value = file.getValue().get(key);
            if (value == null || value.isBlank() || name.startsWith("application-")) {
                continue;
            }
            if (name.equals(MAIN_PROPERTIES)) {
                return Optional.of(value);
            }
            yaml = Optional.of(value);
        }
        return yaml;
    }

    /** Configuration files (any profile) declaring a key equal to {@code prefix} or starting with {@code prefix.}. */
    public List<Path> filesDeclaring(String prefix) {
        return files.entrySet().stream()
                .filter(e -> e.getValue().keySet().stream()
                        .anyMatch(k -> k.equals(prefix) || k.startsWith(prefix + ".")))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static Map<String, String> parse(Path file, String content) {
        Map<String, String> entries = new LinkedHashMap<>();
        if (file.getFileName().toString().endsWith(".properties")) {
            Properties properties = new Properties();
            try {
                properties.load(new StringReader(content));
            } catch (IOException | IllegalArgumentException e) {
                logger.warnf("Could not parse %s: %s", file, e.getMessage());
            }
            properties.stringPropertyNames().forEach(k -> entries.put(k, properties.getProperty(k).trim()));
        } else {
            try {
                // A YAML file may hold several documents, e.g. Spring profile sections separated by ---
                for (Object document : new Yaml(new SafeConstructor(new LoaderOptions())).loadAll(content)) {
                    flatten("", document, entries);
                }
            } catch (RuntimeException e) {
                logger.warnf("Could not parse %s: %s", file, e.getMessage());
            }
        }
        return entries;
    }

    private static void flatten(String prefix, Object node, Map<String, String> out) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((k, v) -> flatten(prefix.isEmpty() ? String.valueOf(k) : prefix + "." + k, v, out));
        } else if (node instanceof Iterable<?> list) {
            out.put(prefix, String.join(",",
                    StreamSupport.stream(list.spliterator(), false).map(String::valueOf).toList()));
        } else if (node != null) {
            out.put(prefix, String.valueOf(node));
        }
    }
}
