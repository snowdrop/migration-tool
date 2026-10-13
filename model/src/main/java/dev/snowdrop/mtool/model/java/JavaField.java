package dev.snowdrop.mtool.model.java;

import dev.snowdrop.mtool.model.analyze.AnnotationInfo;

import java.util.List;
import java.util.Set;

public record JavaField(
        String name,
        String typeName,
        Set<JavaModifier> modifiers,
        List<AnnotationInfo> annotations) {
}
