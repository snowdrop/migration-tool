package dev.snowdrop.mtool.scanner.analyzer;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;

import java.nio.file.Path;
import java.util.List;

public interface JavaAnalyzer {

    AnalysisResult analyze(Path projectPath);

    List<JavaTypeDeclaration> extractTypes(Path projectPath);
}
