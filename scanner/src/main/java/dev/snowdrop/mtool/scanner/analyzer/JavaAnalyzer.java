package dev.snowdrop.mtool.scanner.analyzer;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

import java.nio.file.Path;
import java.util.List;

public interface JavaAnalyzer {

    /**
     * @deprecated Use {@link #extractTypes(Path)} instead, which returns structured
     *             {@link JavaTypeDeclaration} records with {@link dev.snowdrop.mtool.model.analyze.AnnotationInfo}
     *             instead of raw annotation strings.
     */
    @Deprecated
    AnalysisResult analyze(Path projectPath);

    List<JavaTypeDeclaration> extractTypes(Path projectPath);
}
