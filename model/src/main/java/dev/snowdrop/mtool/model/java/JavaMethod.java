package dev.snowdrop.mtool.model.java;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;

import java.util.List;
import java.util.Set;

public record JavaMethod(
        String name,
        String returnType,
        List<JavaParameter> parameters,
        Set<JavaModifier> modifiers,
        List<AnnotationInfo> annotations) {
}
