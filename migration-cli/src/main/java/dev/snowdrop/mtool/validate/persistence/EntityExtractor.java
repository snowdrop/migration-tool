package dev.snowdrop.mtool.validate.persistence;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.codeanalysis.CompilationUnit;
import dev.snowdrop.mtool.model.codeanalysis.FieldDeclaration;
import dev.snowdrop.mtool.model.codeanalysis.TypeDeclaration;
import dev.snowdrop.mtool.model.validate.persistence.EntityModel;
import dev.snowdrop.mtool.model.validate.persistence.FieldModel;
import dev.snowdrop.mtool.model.validate.persistence.IdGenerationModel;
import dev.snowdrop.mtool.model.validate.persistence.RelationshipModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts JPA entity metadata from an AnalysisResult produced by TreeSitterJavaAnalyzer.
 * Adapted from IBM's quarkus-skills EntityExtractor — same logic, different input source.
 */
class EntityExtractor {

    List<EntityModel> extract(AnalysisResult analysis) {
        List<EntityModel> entities = new ArrayList<>();
        if (analysis == null || analysis.getSymbolTable() == null) {
            return entities;
        }

        Map<String, TypeDeclaration> allTypesBySimpleName = new HashMap<>();

        for (Map.Entry<String, CompilationUnit> cuEntry : analysis.getSymbolTable().entrySet()) {
            CompilationUnit cu = cuEntry.getValue();
            if (cu.getTypeDeclarations() == null) {
                continue;
            }

            for (Map.Entry<String, TypeDeclaration> typeEntry : cu.getTypeDeclarations().entrySet()) {
                String qname = typeEntry.getKey();
                int dot = qname.lastIndexOf('.');
                String simpleName = dot >= 0 ? qname.substring(dot + 1) : qname;
                allTypesBySimpleName.put(simpleName, typeEntry.getValue());

                EntityModel entity = buildEntity(qname, typeEntry.getValue(), cu);
                if (entity != null) {
                    entities.add(entity);
                }
            }
        }

        for (EntityModel entity : entities) {
            resolveMappedSuperclass(entity, allTypesBySimpleName);
        }

        return entities;
    }

    private void resolveMappedSuperclass(EntityModel entity, Map<String, TypeDeclaration> allTypes) {
        for (String superclass : orEmpty(entity.getExtendsClasses())) {
            TypeDeclaration parentType = allTypes.get(superclass);
            if (parentType == null) {
                continue;
            }

            boolean isMappedSuperclass = orEmpty(parentType.getAnnotations()).stream()
                    .anyMatch(a -> AnnotationUtils.getAnnotationName(a) != null
                            && "MappedSuperclass".equals(AnnotationUtils.getAnnotationName(a)));
            if (!isMappedSuperclass) {
                continue;
            }

            for (FieldDeclaration field : orEmpty(parentType.getFieldDeclarations())) {
                processField(entity, field);
            }
        }
    }

    private EntityModel buildEntity(String qname, TypeDeclaration jtype, CompilationUnit cu) {
        List<String> annotations = orEmpty(jtype.getAnnotations());
        if (annotations.stream().noneMatch(AnnotationUtils::isEntityAnnotation)) {
            return null;
        }

        int dot = qname.lastIndexOf('.');
        EntityModel entity = new EntityModel();
        entity.setOriginalFile(cu.getFilePath());
        entity.setPackageName(dot >= 0 ? qname.substring(0, dot) : "");
        entity.setClassName(dot >= 0 ? qname.substring(dot + 1) : qname);
        entity.setExtendsClasses(orEmpty(jtype.getExtendsList()));
        entity.setImplementsInterfaces(orEmpty(jtype.getImplementsList()));
        entity.setAnnotations(annotations);

        applyClassAnnotations(entity, annotations);

        List<FieldDeclaration> fields = orEmpty(jtype.getFieldDeclarations());
        for (FieldDeclaration field : fields) {
            if (field.getVariables() == null || field.getVariables().isEmpty()) {
                continue;
            }
            processField(entity, field);
        }

        return entity;
    }

    private void applyClassAnnotations(EntityModel entity, List<String> annotations) {
        for (String ann : annotations) {
            if (ann.contains("@Table")) {
                entity.setTableName(AnnotationUtils.extractAnnotationField(ann, "name"));
            }
        }
    }

    private void processField(EntityModel entity, FieldDeclaration field) {
        String fieldName = field.getVariables().get(0);
        List<String> fa = orEmpty(field.getAnnotations());

        FieldModel fm = new FieldModel();
        fm.setName(fieldName);
        fm.setType(field.getType());
        fm.setAnnotations(fa);
        String isNullable = getAnnotationValue(fa, "@Column", "nullable");
        fm.setNullable(isNullable != null ? "true".equals(isNullable) : null);
        String isUnique = getAnnotationValue(fa, "@Column", "unique");
        fm.setUnique(isUnique != null ? "true".equals(isUnique) : null);
        fm.setColumn(getAnnotationValue(fa, "@Column", "name"));
        fm.setTransientField(fa.stream().anyMatch(a -> a.contains("@Transient")));
        entity.getFields().add(fm);

        if (fa.stream().anyMatch(a -> a.contains("@Id"))) {
            String strategy = AnnotationUtils.simpleName(
                    getAnnotationValue(fa, "@GeneratedValue", "strategy"));
            String generator = getAnnotationValue(fa, "@GeneratedValue", "generator");
            IdGenerationModel idGen = new IdGenerationModel();
            idGen.setStrategy(strategy);
            idGen.setGenerator(generator);
            entity.setIdGeneration(idGen);
        }

        String joinColAnn = fa.stream().filter(a -> a.contains("@JoinColumn")).findFirst().orElse(null);
        for (String ann : fa) {
            for (String rel : Arrays.asList("OneToMany", "ManyToOne", "ManyToMany", "OneToOne")) {
                if (!ann.contains("@" + rel)) {
                    continue;
                }
                String[] normalized = AnnotationUtils.normalizeType(field.getType());
                String fetch = AnnotationUtils.extractAnnotationField(ann, "fetch");
                RelationshipModel rm = new RelationshipModel();
                rm.setType(rel);
                rm.setCollectionType(normalized[0]);
                rm.setTargetEntity(normalized[1]);
                rm.setMappedBy(AnnotationUtils.extractAnnotationField(ann, "mappedBy"));
                rm.setFetch(AnnotationUtils.simpleName(fetch));
                rm.setCascade(AnnotationUtils.parseCascade(ann));
                rm.setColumn(joinColAnn != null
                        ? AnnotationUtils.extractAnnotationField(joinColAnn, "name")
                        : null);
                entity.getRelationships().add(rm);
            }
        }
    }

    private String getAnnotationValue(List<String> annotations, String target, String key) {
        for (String ann : annotations) {
            if (ann.contains(target)) {
                return AnnotationUtils.extractAnnotationField(ann, key);
            }
        }
        return null;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }
}