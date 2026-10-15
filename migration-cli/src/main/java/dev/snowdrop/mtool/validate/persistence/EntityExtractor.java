package dev.snowdrop.mtool.validate.persistence;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;
import dev.snowdrop.mtool.model.java.JavaField;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.model.validate.persistence.EntityModel;
import dev.snowdrop.mtool.model.validate.persistence.FieldModel;
import dev.snowdrop.mtool.model.validate.persistence.IdGenerationModel;
import dev.snowdrop.mtool.model.validate.persistence.RelationshipModel;
import dev.snowdrop.mtool.scanner.fluent.TypeQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extracts JPA entity metadata from a list of JavaTypeDeclarations produced by
 * the Parser type extractor using the Fluent API and structured
 * AnnotationInfo.
 */
class EntityExtractor {

    private static final List<String> RELATIONSHIP_TYPES = List.of(
            "OneToMany", "ManyToOne", "ManyToMany", "OneToOne");
    private static final String ENTITY_ANNOTATION_NAME = "Entity";

    List<EntityModel> extractEntities(List<JavaTypeDeclaration> types) {
        if (types == null || types.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, JavaTypeDeclaration> allTypesBySimpleName = new HashMap<>();
        for (JavaTypeDeclaration type : types) {
            allTypesBySimpleName.put(type.simpleName(), type);
        }

        List<JavaTypeDeclaration> entityTypes = new TypeQuery(types)
                .annotatedWith(ENTITY_ANNOTATION_NAME)
                .toList();

        List<EntityModel> entities = new ArrayList<>();
        for (JavaTypeDeclaration jtype : entityTypes) {
            entities.add(buildEntity(jtype));
        }

        for (EntityModel entity : entities) {
            resolveMappedSuperclass(entity, allTypesBySimpleName);
        }

        return entities;
    }

    private EntityModel buildEntity(JavaTypeDeclaration jtype) {
        EntityModel entity = new EntityModel();
        entity.setOriginalFile(jtype.sourcePath());
        entity.setPackageName(jtype.packageName());
        entity.setClassName(jtype.simpleName());
        entity.setExtendsClasses(
                jtype.superclass() != null ? new ArrayList<>(List.of(jtype.superclass())) : new ArrayList<>());
        entity.setImplementsInterfaces(new ArrayList<>(jtype.interfaces()));
        entity.setAnnotations(jtype.annotations().stream().map(AnnotationInfo::name).toList());

        findAnnotation(jtype.annotations(), "Table")
                .ifPresent(a -> entity.setTableName(stripQuotes(a.getArgument("name"))));

        for (JavaField field : jtype.fields()) {
            processField(entity, field);
        }

        return entity;
    }

    private void resolveMappedSuperclass(EntityModel entity, Map<String, JavaTypeDeclaration> allTypes) {
        for (String superclass : entity.getExtendsClasses()) {
            JavaTypeDeclaration parentType = allTypes.get(superclass);
            if (parentType == null) {
                continue;
            }

            boolean isMappedSuperclass = parentType.annotations().stream()
                    .anyMatch(a -> "MappedSuperclass".equals(a.name()));
            if (!isMappedSuperclass) {
                continue;
            }

            for (JavaField field : parentType.fields()) {
                processField(entity, field);
            }
        }
    }

    private void processField(EntityModel entity, JavaField field) {
        List<AnnotationInfo> annotations = field.annotations();

        FieldModel fm = new FieldModel();
        fm.setName(field.name());
        fm.setType(field.typeName());
        fm.setAnnotations(annotations.stream().map(AnnotationInfo::name).toList());

        findAnnotation(annotations, "Column").ifPresent(col -> {
            fm.setColumn(stripQuotes(col.getArgument("name")));
            String nullable = col.getArgument("nullable");
            fm.setNullable(nullable != null ? "true".equals(nullable) : null);
            String unique = col.getArgument("unique");
            fm.setUnique(unique != null ? "true".equals(unique) : null);
        });

        fm.setTransientField(hasAnnotation(annotations, "Transient"));
        entity.getFields().add(fm);

        if (hasAnnotation(annotations, "Id")) {
            IdGenerationModel idGen = new IdGenerationModel();
            findAnnotation(annotations, "GeneratedValue").ifPresent(gv -> {
                idGen.setStrategy(simpleName(gv.getArgument("strategy")));
                idGen.setGenerator(stripQuotes(gv.getArgument("generator")));
            });
            entity.setIdGeneration(idGen);
        }

        AnnotationInfo joinColumnAnn = findAnnotation(annotations, "JoinColumn").orElse(null);
        for (String relType : RELATIONSHIP_TYPES) {
            findAnnotation(annotations, relType).ifPresent(relAnn -> {
                String[] normalized = normalizeType(field.typeName());
                RelationshipModel rm = new RelationshipModel();
                rm.setType(relType);
                rm.setCollectionType(normalized[0]);
                rm.setTargetEntity(normalized[1]);
                rm.setMappedBy(stripQuotes(relAnn.getArgument("mappedBy")));
                rm.setFetch(simpleName(relAnn.getArgument("fetch")));
                rm.setCascade(parseCascade(relAnn.getArgument("cascade")));
                rm.setColumn(joinColumnAnn != null ? stripQuotes(joinColumnAnn.getArgument("name")) : null);
                entity.getRelationships().add(rm);
            });
        }
    }

    private static Optional<AnnotationInfo> findAnnotation(List<AnnotationInfo> annotations, String name) {
        return annotations.stream().filter(a -> name.equals(a.name())).findFirst();
    }

    private static boolean hasAnnotation(List<AnnotationInfo> annotations, String name) {
        return annotations.stream().anyMatch(a -> name.equals(a.name()));
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String simpleName(String s) {
        if (s == null) {
            return null;
        }
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
    }

    static String[] normalizeType(String typeStr) {
        if (typeStr != null && typeStr.contains("<") && typeStr.contains(">")) {
            int lt = typeStr.indexOf('<');
            int gt = typeStr.indexOf('>');
            String collection = typeStr.substring(0, lt);
            if (collection.contains(".")) {
                collection = collection.substring(collection.lastIndexOf('.') + 1);
            }
            String inner = typeStr.substring(lt + 1, gt);
            return new String[] { collection, inner };
        }
        return new String[] { null, typeStr };
    }

    static List<String> parseCascade(String cascade) {
        if (cascade == null) {
            return null;
        }
        cascade = cascade.replace("{", "").replace("}", "");
        List<String> result = new ArrayList<>();
        for (String c : cascade.split(",")) {
            String trimmed = c.trim();
            int dot = trimmed.lastIndexOf('.');
            result.add(dot >= 0 ? trimmed.substring(dot + 1) : trimmed);
        }
        return result;
    }
}