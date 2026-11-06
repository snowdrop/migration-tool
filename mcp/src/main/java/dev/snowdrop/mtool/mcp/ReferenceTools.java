package dev.snowdrop.mtool.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.ResourceTemplateArg;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;

/**
 * Tools (and resources) giving access to the Spring to Quarkus reference maps.
 */
public class ReferenceTools {

    private static final String MODE_FULL = "full";
    private static final String MODE_COMPAT = "compat";

    @Inject
    ReferenceRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "quarkit_references_list", description = "List the available Spring to Quarkus reference maps "
            + "(annotation, config, dependency) with their sections and number of records.")
    public String list() {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (String name : ReferenceRepository.MAP_NAMES) {
            List<ReferenceItem> items = repository.get(name);
            Set<String> sections = new LinkedHashSet<>();
            items.forEach(i -> sections.add(i.section()));
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("map", name);
            entry.put("records", items.size());
            entry.put("sections", sections);
            maps.add(entry);
        }
        return toJson(maps);
    }

    @Tool(name = "quarkit_reference_lookup", description = "Look up how a Spring annotation, configuration property or dependency "
            + "is migrated to Quarkus. Query by map/section to list all the rows of a section, by map/key to find a row, "
            + "or by map/section/key to find a row within a section. "
            + "Returns the matching rows as JSON: section, spring, quarkus, compat "
            + "(SUPPORTED, NOT_SUPPORTED or SUPPORTED_WITH_CHANGE with the Quarkus Spring compatibility extensions) and notes.")
    public String lookup(
            @ToolArg(name = "map", description = "Reference map to search: 'annotation', 'config' or 'dependency'") String map,
            @ToolArg(name = "section", description = "Section of the map, as returned by quarkit_references_list, e.g. 'Dependency Injection'. "
                    + "When 'key' is omitted, all the rows of the section are returned. Exact match (case-insensitive) is preferred, "
                    + "otherwise a substring match is used", required = false) String section,
            @ToolArg(name = "key", description = "Spring annotation, property or dependency to look for, e.g. '@Primary'. "
                    + "Exact match (case-insensitive) is preferred, otherwise a substring match is used. "
                    + "Restricted to 'section' when provided", required = false) String key,
            @ToolArg(name = "mode", description = "'full' returns all fields, 'compat' returns only spring, quarkus and compat", defaultValue = MODE_FULL) String mode) {

        if (map == null || !ReferenceRepository.MAP_NAMES.contains(map.trim().toLowerCase())) {
            return error("Unknown map '" + map + "'. Use one of: " + ReferenceRepository.MAP_NAMES);
        }
        boolean hasSection = section != null && !section.isBlank();
        boolean hasKey = key != null && !key.isBlank();
        if (!hasSection && !hasKey) {
            return error("At least one of the 'section' or 'key' arguments must be provided");
        }
        if (mode != null && !MODE_FULL.equals(mode) && !MODE_COMPAT.equals(mode)) {
            return error("Unknown mode '" + mode + "'. Use 'full' or 'compat'");
        }

        String mapName = map.trim().toLowerCase();
        List<ReferenceItem> matches = hasSection ? repository.lookupInSection(mapName, section, key)
                : repository.lookup(mapName, key);
        if (MODE_COMPAT.equals(mode)) {
            List<Map<String, Object>> rows = matches.stream().map(i -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("spring", i.spring());
                row.put("quarkus", i.quarkus());
                row.put("compat", i.compat());
                return row;
            }).toList();
            return toJson(rows);
        }
        return toJson(matches);
    }

    @Resource(uri = "mtool://references/annotation-map", name = "annotation-map", description = "Spring to Quarkus annotation map", mimeType = "application/json")
    public String annotationMap() {
        return toJson(repository.get("annotation"));
    }

    @Resource(uri = "mtool://references/config-map", name = "config-map", description = "Spring to Quarkus configuration property map", mimeType = "application/json")
    public String configMap() {
        return toJson(repository.get("config"));
    }

    @Resource(uri = "mtool://references/dependency-map", name = "dependency-map", description = "Spring to Quarkus dependency map", mimeType = "application/json")
    public String dependencyMap() {
        return toJson(repository.get("dependency"));
    }

    @ResourceTemplate(uriTemplate = "mtool://references/{map}/{key}", name = "reference-lookup", description = "Look up a Spring annotation, property or dependency in a reference map (annotation, config or dependency)", mimeType = "application/json")
    public String referenceLookup(@ResourceTemplateArg(name = "map") String map,
            @ResourceTemplateArg(name = "key") String key) {
        if (map != null && key != null && repository.hasSection(map.trim().toLowerCase(), key)) {
            return lookup(map, key, null, MODE_FULL);
        }
        return lookup(map, null, key, MODE_FULL);
    }

    private String error(String message) {
        return toJson(Map.of("error", message));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize reference data", e);
        }
    }
}
