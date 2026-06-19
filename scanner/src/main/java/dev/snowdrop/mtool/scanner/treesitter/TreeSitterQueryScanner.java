package dev.snowdrop.mtool.scanner.treesitter;

import dev.snowdrop.mtool.model.analyze.Config;
import dev.snowdrop.mtool.model.analyze.Match;
import dev.snowdrop.mtool.model.parser.Query;
import dev.snowdrop.mtool.scanner.QueryScanner;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterNode;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterQuery;
import io.roastedroot.treesitter.TreeSitterQueryResult;
import io.roastedroot.treesitter.TreeSitterTree;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class TreeSitterQueryScanner implements QueryScanner {

    private static final Logger logger = Logger.getLogger(TreeSitterQueryScanner.class);

    private static final String SCANNER_TYPE = "treesitter";

    private static final String JAVA_ANNOTATION_QUERY = """
            (marker_annotation name: (identifier) @annotation_name)
            (annotation name: (identifier) @annotation_name)
            """;

    private static final String JAVA_IMPORT_QUERY = """
            (import_declaration
               (scoped_identifier) @package_imported
             )
            """;

    private static final String JAVA_CLASS_QUERY = "(class_declaration name: (identifier) @class_name)";

    private static final String POM_DEPENDENCY_QUERY = """
            (element
              (STag . (Name) @tag.dep (#match? @tag.dep "(dependency|parent)"))
              (content
                (element
                  (STag . (Name) @tag.g (#eq? @tag.g "groupId"))
                  (content . (CharData) @group.id))
                (element
                  (STag . (Name) @tag.a (#eq? @tag.a "artifactId"))
                  (content . (CharData) @artifact.id))
              )) @dependency.block
            """;

    private static String JAVA_SOURCE_GLOB_PATTERN = "glob:**/src/{main,test}/java/**/*.java";

    @Override
    public List<Match> scansCodeFor(Config config, Query query) {
        String key = query.fileType() + "." + query.symbol();
        logger.infof("TreeSitter scanner executing for query %s", key);

        return switch (key) {
            case "java.class" -> scanJavaClasses(config, query);
            case "java.annotation" -> scanJavaAnnotations(config, query);
            case "java.import" -> scanJavaImport(config, query);
            case "pom.dependency" -> scanPomDependencies(config, query);
            default -> throw new IllegalArgumentException("Unsupported query: " + key);
        };
    }

    @Override
    @Deprecated
    public List<Match> executeQueries(Config config, Set<Query> queries) {
        return List.of();
    }

    @Override
    public String getScannerType() {
        return SCANNER_TYPE;
    }

    @Override
    public boolean supports(Query query) {
        String fileType = query.fileType();
        String symbol = query.symbol();
        return (fileType.equals("java") && (symbol.equals("annotation") || symbol.equals("class") || symbol.equals("import")))
                || (fileType.equals("pom") && symbol.equals("dependency"));
    }

    private List<Match> scanJavaClasses(Config config, Query query) {
        List<Match> matches = new ArrayList<>();
        List<Path> javaFiles = findFiles(Paths.get(config.appPath()), JAVA_SOURCE_GLOB_PATTERN);

        try (TreeSitter ts = TreeSitter.create();
                TreeSitterParser parser = ts.newParser(Language.JAVA);
                TreeSitterQuery tsQuery = ts.newQuery(Language.JAVA, JAVA_CLASS_QUERY)) {

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);

                try (TreeSitterTree tree = parser.parseString(source)) {
                    List<TreeSitterQueryResult> results = tsQuery.exec(tree.rootNode(), source);
                    matches.addAll(generateMatchesFromResults(results, query, source, config.appPath(), javaFile));
                }
            }
        } catch (IOException e) {
            logger.errorf("Error scanning Java files for classes: %s", e.getMessage());
        }

        logger.infof("Found %d class matches", matches.size());
        return matches;
    }

    private List<Match> scanJavaAnnotations(Config config, Query query) {
        List<Match> matches = new ArrayList<>();
        List<Path> javaFiles = findFiles(Paths.get(config.appPath()), JAVA_SOURCE_GLOB_PATTERN);

        try (TreeSitter ts = TreeSitter.create();
                TreeSitterParser parser = ts.newParser(Language.JAVA);
                TreeSitterQuery tsQuery = ts.newQuery(Language.JAVA, JAVA_ANNOTATION_QUERY)) {

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);

                try (TreeSitterTree tree = parser.parseString(source)) {
                    TreeSitterNode root = tree.rootNode();

                    List<TreeSitterQueryResult> results = tsQuery.exec(root, source);
                    matches.addAll(generateMatchesFromResults(results, query, source, config.appPath(), javaFile));
                }
            }
        } catch (IOException e) {
            logger.errorf("Error scanning Java files for annotations: %s", e.getMessage());
        }
        logger.infof("Found %d annotation matches.", matches.size());
        return matches;
    }

    private List<Match> scanJavaImport(Config config, Query query) {
        List<Match> matches = new ArrayList<>();
        List<Path> javaFiles = findFiles(Paths.get(config.appPath()), JAVA_SOURCE_GLOB_PATTERN);

        try (TreeSitter ts = TreeSitter.create();
                TreeSitterParser parser = ts.newParser(Language.JAVA);
                TreeSitterQuery tsQuery = ts.newQuery(Language.JAVA, JAVA_IMPORT_QUERY)) {

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);

                try (TreeSitterTree tree = parser.parseString(source)) {
                    List<TreeSitterQueryResult> results = tsQuery.exec(tree.rootNode(), source);
                    matches.addAll(generateMatchesFromResults(results, query, source, config.appPath(), javaFile));
                }
            }
        } catch (IOException e) {
            logger.errorf("Error scanning Java files for import: %s", e.getMessage());
        }

        logger.infof("Found %d import package matches", matches.size());
        return matches;
    }

    private List<Match> generateMatchesFromResults(List<TreeSitterQueryResult> results, Query query, String source,
            String appPath, Path filePath) {
        List<Match> matches = new ArrayList<>();
        for (TreeSitterQueryResult result : results) {
            String entityName = source.substring(result.node().startByte(), result.node().endByte());

            String relativePath = Paths.get(appPath).relativize(filePath).toString();
            String formatted = formatResult(relativePath, result.node(), entityName);
            matches.add(new Match(
                    query.fileType() + "-" + query.symbol(),
                    SCANNER_TYPE,
                    formatted));
        }
        return matches;
    }

    private String formatResult(String relativePath, TreeSitterNode node, String entityName) {
        return String.format("Path: %s, start: (%d, %d), end: (%d-%d), text: %s",
                relativePath,
                node.startRow() + 1,
                node.startColumn() + 1,
                node.endRow() + 1,
                node.endColumn() + 1,
                entityName);
    }

    private List<Match> scanPomDependencies(Config config, Query query) {
        String gavs = query.keyValues().get("gavs");
        if (gavs == null) {
            logger.warn("No 'gavs' key provided for pom.dependency query");
            return List.of();
        }

        String[] gavParts = gavs.split(":");
        String targetGroupId = gavParts.length > 0 ? gavParts[0] : null;
        String targetArtifactId = gavParts.length > 1 ? gavParts[1] : null;

        List<Match> matches = new ArrayList<>();
        List<Path> pomFiles = findFiles(Paths.get(config.appPath()), "glob:**/pom.xml");

        try (TreeSitter ts = TreeSitter.create();
                TreeSitterParser parser = ts.newParser(Language.XML)) {

            for (Path pomFile : pomFiles) {
                String source = Files.readString(pomFile);

                try (TreeSitterTree tree = parser.parseString(source);
                        TreeSitterQuery tsQuery = ts.newQuery(Language.XML, POM_DEPENDENCY_QUERY)) {

                    List<TreeSitterQueryResult> results = tsQuery.exec(tree.rootNode(), source);

                    String currentGroupId = null;
                    String currentArtifactId = null;
                    TreeSitterNode blockNode = null;

                    for (TreeSitterQueryResult result : results) {
                        switch (result.name()) {
                            case "dependency.block" -> blockNode = result.node();
                            case "group.id" -> currentGroupId = source.substring(
                                    result.node().startByte(), result.node().endByte()).trim();
                            case "artifact.id" -> {
                                currentArtifactId = source.substring(
                                        result.node().startByte(), result.node().endByte()).trim();

                                if (matchesGav(currentGroupId, currentArtifactId, targetGroupId, targetArtifactId)
                                        && blockNode != null) {
                                    int line = blockNode.startRow() + 1;
                                    String relativePath = Paths.get(config.appPath()).relativize(pomFile).toString();
                                    String formatted = String.format("%s:%d | %s:%s",
                                            relativePath, line, currentGroupId, currentArtifactId);
                                    matches.add(new Match(
                                            query.fileType() + "-" + query.symbol(),
                                            SCANNER_TYPE,
                                            formatted));
                                }

                                currentGroupId = null;
                                currentArtifactId = null;
                                blockNode = null;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.errorf("Error scanning POM files for dependencies: %s", e.getMessage());
        }

        logger.infof("Found %d dependency matches for '%s'", matches.size(), gavs);
        return matches;
    }

    private boolean matchesGav(String groupId, String artifactId, String targetGroupId, String targetArtifactId) {
        if (groupId == null) {
            return false;
        }
        boolean groupMatch = targetGroupId.equals(groupId);
        if (targetArtifactId == null || targetArtifactId.isEmpty()) {
            return groupMatch;
        }
        return groupMatch && targetArtifactId.equals(artifactId);
    }

    private List<Path> findFiles(Path startPath, String globPattern) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(startPath)) {
            var matcher = java.nio.file.FileSystems.getDefault().getPathMatcher(globPattern);
            paths.filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p))
                    .forEach(files::add);
        } catch (IOException e) {
            logger.errorf("Error walking file tree from %s: %s", startPath, e.getMessage());
        }
        return files;
    }
}
