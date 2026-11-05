package dev.snowdrop.mtool.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Loads the Spring to Quarkus reference maps (annotation, config, dependency) once at startup.
 * <p>
 * For each map, the file {@code <map>-map.json} is looked up first in the directory or jar given by the
 * {@code mtool.references.path} property, then on the classpath under {@code references/}.
 */
@ApplicationScoped
public class ReferenceRepository {

    private static final Logger LOG = Logger.getLogger(ReferenceRepository.class);
    private static final TypeReference<List<ReferenceItem>> ITEMS = new TypeReference<>() {
    };

    public static final List<String> MAP_NAMES = List.of("annotation", "config", "dependency");

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "mtool.references.path")
    Optional<String> referencesPath;

    private final Map<String, List<ReferenceItem>> referenceData = new LinkedHashMap<>();

    @Startup
    void onStartup() {
        for (String map : MAP_NAMES) {
            String fileName = map + "-map.json";
            List<ReferenceItem> items = referencesPath.filter(p -> !p.isBlank())
                    .flatMap(p -> loadFromPath(Paths.get(p.trim()), fileName))
                    .or(() -> loadFromClasspath(fileName))
                    .orElse(null);
            if (items == null) {
                LOG.warnf("Reference file not found: %s", fileName);
                continue;
            }
            referenceData.put(map, items);
            LOG.infof("Loaded reference file: %s (%d records)", fileName, items.size());
        }
    }

    public boolean has(String map) {
        return referenceData.containsKey(map);
    }

    public List<ReferenceItem> get(String map) {
        return referenceData.getOrDefault(map, Collections.emptyList());
    }

    /**
     * Returns the rows of {@code map} whose Spring side equals {@code key} (case-insensitive). If there is no exact
     * match, returns the rows whose Spring side contains {@code key}.
     */
    public List<ReferenceItem> lookup(String map, String key) {
        String needle = key.trim().toLowerCase(Locale.ROOT);
        List<ReferenceItem> items = get(map);
        List<ReferenceItem> exact = items.stream()
                .filter(i -> i.spring().toLowerCase(Locale.ROOT).equals(needle))
                .toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return items.stream()
                .filter(i -> i.spring().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private Optional<List<ReferenceItem>> loadFromPath(Path path, String fileName) {
        try {
            if (Files.isDirectory(path)) {
                Path file = path.resolve(fileName);
                if (!Files.isRegularFile(file)) {
                    return Optional.empty();
                }
                try (InputStream is = Files.newInputStream(file)) {
                    LOG.infof("Using %s from %s", fileName, path);
                    return Optional.of(objectMapper.readValue(is, ITEMS));
                }
            }
            if (Files.isRegularFile(path)) { // a jar (or zip)
                try (FileSystem zipFs = FileSystems.newFileSystem(path)) {
                    Path file = zipFs.getPath("/", "references", fileName);
                    if (!Files.isRegularFile(file)) {
                        file = zipFs.getPath("/", fileName);
                    }
                    if (!Files.isRegularFile(file)) {
                        return Optional.empty();
                    }
                    try (InputStream is = Files.newInputStream(file)) {
                        LOG.infof("Using %s from %s", fileName, path);
                        return Optional.of(objectMapper.readValue(is, ITEMS));
                    }
                }
            }
            LOG.warnf("mtool.references.path does not exist: %s", path);
        } catch (IOException | RuntimeException e) {
            LOG.errorf(e, "Failed to read %s from %s", fileName, path);
        }
        return Optional.empty();
    }

    private Optional<List<ReferenceItem>> loadFromClasspath(String fileName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream("references/" + fileName)) {
            if (is == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(is, ITEMS));
        } catch (IOException e) {
            LOG.errorf(e, "Failed to parse classpath resource: references/%s", fileName);
            return Optional.empty();
        }
    }
}
