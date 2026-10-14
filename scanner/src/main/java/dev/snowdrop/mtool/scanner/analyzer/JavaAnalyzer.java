package dev.snowdrop.mtool.scanner.analyzer;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;

import java.nio.file.Path;

public interface JavaAnalyzer {

    AnalysisResult analyze(Path projectPath);
}
