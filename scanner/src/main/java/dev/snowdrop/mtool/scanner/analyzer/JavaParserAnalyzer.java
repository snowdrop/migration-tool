package dev.snowdrop.mtool.scanner.analyzer;

import com.github.javaparser.StaticJavaParser;
import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.codeanalysis.CompilationUnit;
import dev.snowdrop.mtool.model.codeanalysis.FieldDeclaration;
import dev.snowdrop.mtool.model.codeanalysis.TypeDeclaration;
import dev.snowdrop.mtool.model.java.JavaSource;
import dev.snowdrop.mtool.model.java.JavaTypeDeclaration;
import dev.snowdrop.mtool.scanner.javaparser.index.JavaParserTypeExtractor;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaParserAnalyzer implements JavaAnalyzer {

    private static final Logger logger = Logger.getLogger(JavaParserAnalyzer.class);

    @Override
    public AnalysisResult analyze(Path projectPath) {
        AnalysisResult result = new AnalysisResult();
        List<Path> javaFiles = findJavaFiles(projectPath);
        logger.infof("JavaParser analyzing %d Java files in %s", javaFiles.size(), projectPath);

        for (Path file : javaFiles) {
            try {
                String source = Files.readString(file);
                com.github.javaparser.ast.CompilationUnit jpCu = StaticJavaParser.parse(source);
                CompilationUnit cu = extractCompilationUnit(file, jpCu);
                String relativePath = projectPath.relativize(file).toString();
                result.getSymbolTable().put(relativePath, cu);
            } catch (IOException e) {
                logger.warnf("Failed to read file %s: %s", file, e.getMessage());
            } catch (Exception e) {
                logger.warnf("Failed to parse file %s: %s", file, e.getMessage());
            }
        }

        return result;
    }

    private CompilationUnit extractCompilationUnit(Path file, com.github.javaparser.ast.CompilationUnit jpCu) {
        CompilationUnit cu = new CompilationUnit();
        cu.setFilePath(file.toString());
        cu.setPackageName(jpCu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse(""));

        for (com.github.javaparser.ast.ImportDeclaration imp : jpCu.getImports()) {
            cu.getImports().add(imp.getNameAsString());
        }

        for (com.github.javaparser.ast.body.TypeDeclaration<?> jpType : jpCu.getTypes()) {
            TypeDeclaration type = extractTypeDeclaration(jpType);
            String qname = cu.getPackageName().isEmpty()
                    ? jpType.getNameAsString()
                    : cu.getPackageName() + "." + jpType.getNameAsString();
            cu.getTypeDeclarations().put(qname, type);
        }

        return cu;
    }

    private TypeDeclaration extractTypeDeclaration(com.github.javaparser.ast.body.TypeDeclaration<?> jpType) {
        TypeDeclaration type = new TypeDeclaration();

        if (jpType instanceof com.github.javaparser.ast.body.ClassOrInterfaceDeclaration cid) {
            type.setInterface(cid.isInterface());

            if (!cid.isInterface()) {
                cid.getExtendedTypes().forEach(
                        ext -> type.getExtendsList().add(ext.getNameAsString()));
                cid.getImplementedTypes().forEach(
                        impl -> type.getImplementsList().add(impl.getNameAsString()));
            } else {
                cid.getExtendedTypes().forEach(
                        ext -> type.getExtendsList().add(ext.getNameAsString()));
            }
        } else if (jpType instanceof com.github.javaparser.ast.body.EnumDeclaration ed) {
            type.setInterface(false);
            ed.getImplementedTypes().forEach(
                    impl -> type.getImplementsList().add(impl.getNameAsString()));
        }

        List<String> annotations = new ArrayList<>();
        for (com.github.javaparser.ast.expr.AnnotationExpr ann : jpType.getAnnotations()) {
            annotations.add(ann.toString());
        }
        type.setAnnotations(annotations);

        type.setFieldDeclarations(extractFields(jpType));

        return type;
    }

    private List<FieldDeclaration> extractFields(com.github.javaparser.ast.body.TypeDeclaration<?> jpType) {
        List<FieldDeclaration> fields = new ArrayList<>();

        for (com.github.javaparser.ast.body.FieldDeclaration jpField : jpType.getFields()) {
            FieldDeclaration field = new FieldDeclaration();

            List<String> annotations = new ArrayList<>();
            for (com.github.javaparser.ast.expr.AnnotationExpr ann : jpField.getAnnotations()) {
                annotations.add(ann.toString());
            }
            field.setAnnotations(annotations);
            field.setType(jpField.getElementType().asString());

            List<String> variables = new ArrayList<>();
            for (com.github.javaparser.ast.body.VariableDeclarator var : jpField.getVariables()) {
                variables.add(var.getNameAsString());
            }
            field.setVariables(variables);

            fields.add(field);
        }

        return fields;
    }

    @Override
    public List<JavaTypeDeclaration> extractTypes(Path projectPath) {
        List<Path> javaFiles = findJavaFiles(projectPath);
        List<JavaTypeDeclaration> allTypes = new ArrayList<>();

        for (Path file : javaFiles) {
            try {
                String source = Files.readString(file);
                JavaSource javaSource = new JavaSource(file.toString(), source);
                allTypes.addAll(JavaParserTypeExtractor.extract(javaSource));
            } catch (IOException e) {
                logger.warnf("Failed to read file %s: %s", file, e.getMessage());
            } catch (Exception e) {
                logger.warnf("Failed to parse file %s: %s", file, e.getMessage());
            }
        }

        return allTypes;
    }

    @Override
    public String toString() {
        return "JavaParser";
    }
}
