package dev.snowdrop.mtool.validate.persistence;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.validate.ValidationReport;
import dev.snowdrop.mtool.model.validate.ValidationResult;
import dev.snowdrop.mtool.model.validate.ValidationResult.Status;
import dev.snowdrop.mtool.model.validate.ValidationRule;
import dev.snowdrop.mtool.model.validate.persistence.EntityModel;
import dev.snowdrop.mtool.model.validate.persistence.FieldModel;
import dev.snowdrop.mtool.model.validate.persistence.IdGenerationModel;
import dev.snowdrop.mtool.model.validate.persistence.RelationshipModel;
import dev.snowdrop.mtool.scanner.treesitter.TreeSitterJavaAnalyzer;
import dev.snowdrop.mtool.validate.Validator;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Validates persistence migration by comparing JPA entities between the original
 * Spring project and the migrated Quarkus project.
 *
 * Uses TreeSitterJavaAnalyzer (instead of CLDK) to extract entity metadata,
 * then compares: entity count, table names, ID generation, fields, relationships.
 *
 * Adapted from IBM's quarkus-skills PersistenceValidator.
 */
public class PersistenceValidator implements Validator {

    private static final Logger logger = Logger.getLogger(PersistenceValidator.class);

    private static final Map<String, String> DEFAULT_FETCH = Map.of(
            "ManyToOne", "EAGER",
            "OneToMany", "LAZY",
            "OneToOne", "EAGER",
            "ManyToMany", "LAZY");

    private final Path springProjectPath;
    private final boolean skipCompile;

    public PersistenceValidator(Path springProjectPath, boolean skipCompile) {
        this.springProjectPath = springProjectPath;
        this.skipCompile = skipCompile;
    }

    @Override
    public String name() {
        return "persistence";
    }

    @Override
    public ValidationReport validate(Path quarkusProjectPath) {
        ValidationReport report = new ValidationReport(name());
        TreeSitterJavaAnalyzer analyzer = new TreeSitterJavaAnalyzer();
        EntityExtractor extractor = new EntityExtractor();

        logger.infof("Analyzing Spring project: %s", springProjectPath);
        AnalysisResult springAnalysis = analyzer.analyze(springProjectPath);
        List<EntityModel> springEntities = extractor.extract(springAnalysis);
        normalizeEntities(springEntities);

        logger.infof("Analyzing Quarkus project: %s", quarkusProjectPath);
        AnalysisResult quarkusAnalysis = analyzer.analyze(quarkusProjectPath);
        List<EntityModel> quarkusEntities = extractor.extract(quarkusAnalysis);
        normalizeEntities(quarkusEntities);

        logger.infof("Found %d Spring entities, %d Quarkus entities",
                springEntities.size(), quarkusEntities.size());

        validateEntityCount(springEntities, quarkusEntities, report);
        validateEntities(springEntities, quarkusEntities, report);

        if (!skipCompile) {
            checkMavenCompile(report, quarkusProjectPath);
        }

        return report;
    }

    private void normalizeEntities(List<EntityModel> entities) {
        for (EntityModel entity : entities) {
            for (RelationshipModel rel : entity.getRelationships()) {
                if (rel.getFetch() == null) {
                    rel.setFetch(DEFAULT_FETCH.get(rel.getType()));
                }
            }
            for (FieldModel field : entity.getFields()) {
                if (field.getColumn() != null) {
                    field.setColumn(field.getColumn().toUpperCase());
                }
            }
            for (RelationshipModel rel : entity.getRelationships()) {
                if (rel.getColumn() != null) {
                    rel.setColumn(rel.getColumn().toUpperCase());
                }
            }
        }
    }

    private void validateEntityCount(List<EntityModel> spring, List<EntityModel> quarkus,
            ValidationReport report) {
        int springCount = spring.size();
        int quarkusCount = quarkus.size();

        if (springCount == quarkusCount) {
            report.add(new ValidationResult(
                    new ValidationRule("Entity count", "Entity count matches between Spring and Quarkus"),
                    Status.PASSED,
                    String.format("Entity count matches: %d entities in both projects", springCount)));
        } else {
            report.add(new ValidationResult(
                    new ValidationRule("Entity count", "Entity count matches between Spring and Quarkus"),
                    Status.FAILED,
                    String.format("Entity count mismatch: Spring has %d entities, Quarkus has %d", springCount,
                            quarkusCount)));
        }
    }

    private void validateEntities(List<EntityModel> spring, List<EntityModel> quarkus,
            ValidationReport report) {
        Map<String, EntityModel> springMap = spring.stream()
                .collect(Collectors.toMap(EntityModel::getClassName, e -> e));
        Map<String, EntityModel> quarkusMap = quarkus.stream()
                .collect(Collectors.toMap(EntityModel::getClassName, e -> e));

        List<String> missing = springMap.keySet().stream()
                .filter(name -> !quarkusMap.containsKey(name))
                .toList();

        if (missing.isEmpty()) {
            report.add(new ValidationResult(
                    new ValidationRule("All entities migrated", "All Spring entities exist in Quarkus"),
                    Status.PASSED,
                    "All Spring entities found in Quarkus project"));
        } else {
            report.add(new ValidationResult(
                    new ValidationRule("All entities migrated", "All Spring entities exist in Quarkus"),
                    Status.FAILED,
                    "Missing entities in Quarkus: " + String.join(", ", missing)));
        }

        for (String entityName : springMap.keySet()) {
            if (quarkusMap.containsKey(entityName)) {
                compareEntity(springMap.get(entityName), quarkusMap.get(entityName), report);
            }
        }
    }

    private void compareEntity(EntityModel spring, EntityModel quarkus, ValidationReport report) {
        compareTableName(spring, quarkus, report);
        compareIdGeneration(spring, quarkus, report);
        compareFields(spring, quarkus, report);
        compareRelationships(spring, quarkus, report);
    }

    private void compareTableName(EntityModel spring, EntityModel quarkus, ValidationReport report) {
        String ruleName = spring.getClassName() + " table name";
        if (Objects.equals(spring.getTableName(), quarkus.getTableName())) {
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "Table name matches"),
                    Status.PASSED,
                    String.format("Table name %s validated", spring.getTableName())));
        } else if (spring.getTableName() == null) {
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "Table name matches"),
                    Status.PASSED,
                    String.format("Implicit '%s' -> explicit '%s' (naming convention change allowed)",
                            spring.getClassName(), quarkus.getTableName())));
        } else if (isConstantReference(spring.getTableName()) || isConstantReference(quarkus.getTableName())) {
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "Table name matches"),
                    Status.WARNING,
                    String.format("Cannot resolve constant reference: %s vs %s",
                            spring.getTableName(), quarkus.getTableName())));
        } else {
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "Table name matches"),
                    Status.FAILED,
                    String.format("Table mismatch: %s -> %s", spring.getTableName(), quarkus.getTableName())));
        }
    }

    private void compareIdGeneration(EntityModel spring, EntityModel quarkus, ValidationReport report) {
        IdGenerationModel springId = spring.getIdGeneration();
        IdGenerationModel quarkusId = quarkus.getIdGeneration();
        String ruleName = spring.getClassName() + " ID generation";

        if (springId == null && quarkusId == null) {
            return;
        }

        if (springId != null && quarkusId == null && extendsPanacheEntity(quarkus)) {
            String springStrategy = springId.getStrategy();
            if (springStrategy == null || "AUTO".equals(springStrategy)) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "ID generation strategy matches"),
                        Status.PASSED,
                        "ID managed by PanacheEntity (default strategy AUTO matches Spring)"));
            } else {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "ID generation strategy matches"),
                        Status.WARNING,
                        String.format("PanacheEntity uses default strategy AUTO, Spring used %s",
                                springStrategy)));
            }
            return;
        }

        if (springId != null && quarkusId != null
                && Objects.equals(springId.getStrategy(), quarkusId.getStrategy())) {
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "ID generation strategy matches"),
                    Status.PASSED,
                    String.format("ID strategy %s validated", springId.getStrategy())));
        } else {
            String from = springId != null ? springId.getStrategy() : "none";
            String to = quarkusId != null ? quarkusId.getStrategy() : "none";
            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "ID generation strategy matches"),
                    Status.FAILED,
                    String.format("ID strategy changed: %s -> %s", from, to)));
        }
    }

    private void compareFields(EntityModel spring, EntityModel quarkus, ValidationReport report) {
        Map<String, FieldModel> springFields = spring.getFields().stream()
                .filter(f -> !f.isTransientField())
                .collect(Collectors.toMap(FieldModel::getName, f -> f));
        Map<String, FieldModel> quarkusFields = quarkus.getFields().stream()
                .filter(f -> !f.isTransientField())
                .collect(Collectors.toMap(FieldModel::getName, f -> f));

        for (Map.Entry<String, FieldModel> entry : springFields.entrySet()) {
            String fieldName = entry.getKey();
            FieldModel springField = entry.getValue();
            String ruleName = spring.getClassName() + "." + fieldName;

            if (!quarkusFields.containsKey(fieldName)) {
                if ("id".equals(fieldName) && extendsPanacheEntity(quarkus)) {
                    report.add(new ValidationResult(
                            new ValidationRule(ruleName, "Field exists in Quarkus"),
                            Status.PASSED,
                            "Field 'id' inherited from PanacheEntity"));
                    continue;
                }
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Field exists in Quarkus"),
                        Status.FAILED,
                        String.format("Missing field %s", fieldName)));
                continue;
            }

            FieldModel quarkusField = quarkusFields.get(fieldName);

            if (!Objects.equals(simpleClassName(springField.getType()),
                    simpleClassName(quarkusField.getType()))) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Field type matches"),
                        Status.FAILED,
                        String.format("Type mismatch: %s -> %s", springField.getType(), quarkusField.getType())));
                continue;
            }

            if (!Objects.equals(springField.getColumn(), quarkusField.getColumn())
                    && springField.getColumn() != null) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Column mapping matches"),
                        Status.FAILED,
                        String.format("Column mismatch: %s -> %s", springField.getColumn(),
                                quarkusField.getColumn())));
                continue;
            }

            report.add(new ValidationResult(
                    new ValidationRule(ruleName, "Field validated"),
                    Status.PASSED,
                    String.format("Field %s validated", fieldName)));
        }
    }

    private void compareRelationships(EntityModel spring, EntityModel quarkus, ValidationReport report) {
        Map<String, RelationshipModel> springRels = spring.getRelationships().stream()
                .collect(Collectors.toMap(this::relationshipKey, r -> r));
        Map<String, RelationshipModel> quarkusRels = quarkus.getRelationships().stream()
                .collect(Collectors.toMap(this::relationshipKey, r -> r));

        for (Map.Entry<String, RelationshipModel> entry : springRels.entrySet()) {
            String key = entry.getKey();
            RelationshipModel springRel = entry.getValue();
            String relId = String.format("%s -> %s", springRel.getType(), springRel.getTargetEntity());
            String ruleName = spring.getClassName() + " relationship " + relId;

            if (!quarkusRels.containsKey(key)) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Relationship exists"),
                        Status.FAILED,
                        String.format("Missing relationship %s", relId)));
                continue;
            }

            RelationshipModel quarkusRel = quarkusRels.get(key);
            boolean ok = true;

            if (!Objects.equals(springRel.getMappedBy(), quarkusRel.getMappedBy())) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "mappedBy matches"),
                        Status.FAILED,
                        String.format("mappedBy mismatch: %s -> %s",
                                springRel.getMappedBy(), quarkusRel.getMappedBy())));
                ok = false;
            }

            if (!Objects.equals(springRel.getCollectionType(), quarkusRel.getCollectionType())) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Collection type matches"),
                        Status.FAILED,
                        String.format("Collection type mismatch: %s -> %s",
                                springRel.getCollectionType(), quarkusRel.getCollectionType())));
                ok = false;
            }

            if (ok) {
                report.add(new ValidationResult(
                        new ValidationRule(ruleName, "Relationship validated"),
                        Status.PASSED,
                        String.format("Relationship %s validated", relId)));
            }
        }
    }

    private void checkMavenCompile(ValidationReport report, Path projectPath) {
        ValidationRule rule = new ValidationRule("mvn compile", "Project compiles successfully");
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "-B", "--no-transfer-progress");
            pb.directory(projectPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder outputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                report.add(new ValidationResult(rule, Status.FAILED, "Maven compile timed out after 180s"));
                return;
            }

            String output = outputBuilder.toString();
            if (process.exitValue() == 0 && output.toUpperCase().contains("BUILD SUCCESS")) {
                report.add(new ValidationResult(rule, Status.PASSED, "mvn compile — BUILD SUCCESS"));
            } else {
                String errors = output.lines()
                        .filter(l -> l.contains("[ERROR]"))
                        .limit(5)
                        .collect(Collectors.joining("\n  "));
                report.add(new ValidationResult(rule, Status.FAILED,
                        "mvn compile — BUILD FAILURE\n  " + errors));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            report.add(new ValidationResult(rule, Status.FAILED, "Maven compile interrupted"));
        } catch (IOException e) {
            report.add(new ValidationResult(rule, Status.FAILED,
                    "Maven (mvn) not found in PATH"));
        }
    }

    private String relationshipKey(RelationshipModel rel) {
        String target = simpleClassName(rel.getTargetEntity());
        return String.format("%s|%s|%s", rel.getType(), target, rel.getColumn());
    }

    private static boolean isConstantReference(String value) {
        if (value == null) {
            return false;
        }
        return value.contains(".");
    }

    private static boolean extendsPanacheEntity(EntityModel entity) {
        return entity.getExtendsClasses().stream()
                .anyMatch(s -> "PanacheEntity".equals(simpleClassName(s)));
    }

    private static String simpleClassName(String fullName) {
        if (fullName == null) {
            return null;
        }
        int dot = fullName.lastIndexOf('.');
        return dot >= 0 ? fullName.substring(dot + 1) : fullName;
    }
}