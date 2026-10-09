package dev.snowdrop.mtool.scanner.treesitter;

import dev.snowdrop.mtool.model.codeanalysis.AnalysisResult;
import dev.snowdrop.mtool.model.codeanalysis.CompilationUnit;
import dev.snowdrop.mtool.model.codeanalysis.FieldDeclaration;
import dev.snowdrop.mtool.model.codeanalysis.TypeDeclaration;
import io.roastedroot.treesitter.Language;
import io.roastedroot.treesitter.TreeSitter;
import io.roastedroot.treesitter.TreeSitterNode;
import io.roastedroot.treesitter.TreeSitterParser;
import io.roastedroot.treesitter.TreeSitterTree;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Replaces CLDK's CodeAnalyzer: parses Java source files using TreeSitter
 * and builds an AnalysisResult with the same structure that the Extractors consume.
 */
public class TreeSitterJavaAnalyzer {

    private static final Logger logger = Logger.getLogger(TreeSitterJavaAnalyzer.class);

    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "target", "build", ".git", ".svn", "node_modules", ".m2");

    public AnalysisResult analyze(Path projectPath) {
        AnalysisResult result = new AnalysisResult();
        List<Path> javaFiles = findJavaFiles(projectPath);
        logger.infof("TreeSitter analyzing %d Java files in %s", javaFiles.size(), projectPath);

        try (TreeSitter ts = TreeSitter.create();
                TreeSitterParser parser = ts.newParser(Language.JAVA)) {

            for (Path file : javaFiles) {
                String source = Files.readString(file);
                try (TreeSitterTree tree = parser.parseString(source)) {
                    CompilationUnit cu = extractCompilationUnit(file, tree.rootNode(), source);
                    String relativePath = projectPath.relativize(file).toString();
                    result.getSymbolTable().put(relativePath, cu);
                }
            }
        } catch (IOException e) {
            logger.errorf("Error analyzing project: %s", e.getMessage());
        }

        return result;
    }

    private CompilationUnit extractCompilationUnit(Path file, TreeSitterNode root, String source) {
        CompilationUnit cu = new CompilationUnit();
        cu.setFilePath(file.toString());
        cu.setPackageName("");

        for (int i = 0; i < root.namedChildCount(); i++) {
            TreeSitterNode child = root.namedChild(i);
            switch (child.type()) {
                case "package_declaration" -> cu.setPackageName(extractPackageName(child, source));
                case "import_declaration" -> cu.getImports().add(extractImportPath(child, source));
                case "class_declaration" -> {
                    TypeDeclaration type = extractClassDeclaration(child, source);
                    String qname = qualifiedName(cu.getPackageName(), extractIdentifier(child, source));
                    cu.getTypeDeclarations().put(qname, type);
                }
                case "interface_declaration" -> {
                    TypeDeclaration type = extractInterfaceDeclaration(child, source);
                    String qname = qualifiedName(cu.getPackageName(), extractIdentifier(child, source));
                    cu.getTypeDeclarations().put(qname, type);
                }
            }
        }

        return cu;
    }

    private TypeDeclaration extractClassDeclaration(TreeSitterNode classNode, String source) {
        TypeDeclaration type = new TypeDeclaration();
        type.setInterface(false);

        TreeSitterNode modifiers = findChild(classNode, "modifiers");
        type.setAnnotations(extractAnnotations(modifiers, source));

        TreeSitterNode superclass = findChild(classNode, "superclass");
        if (superclass != null) {
            type.getExtendsList().add(nodeText(superclass.namedChild(0), source));
        }

        TreeSitterNode interfaces = findChild(classNode, "super_interfaces");
        if (interfaces != null) {
            TreeSitterNode typeList = findChild(interfaces, "type_list");
            if (typeList != null) {
                for (int i = 0; i < typeList.namedChildCount(); i++) {
                    type.getImplementsList().add(nodeText(typeList.namedChild(i), source));
                }
            }
        }

        TreeSitterNode body = findChild(classNode, "class_body");
        if (body != null) {
            type.setFieldDeclarations(extractFields(body, source));
        }

        return type;
    }

    private TypeDeclaration extractInterfaceDeclaration(TreeSitterNode interfaceNode, String source) {
        TypeDeclaration type = new TypeDeclaration();
        type.setInterface(true);

        TreeSitterNode modifiers = findChild(interfaceNode, "modifiers");
        type.setAnnotations(extractAnnotations(modifiers, source));

        TreeSitterNode extendsInterfaces = findChild(interfaceNode, "extends_interfaces");
        if (extendsInterfaces != null) {
            TreeSitterNode typeList = findChild(extendsInterfaces, "type_list");
            if (typeList != null) {
                for (int i = 0; i < typeList.namedChildCount(); i++) {
                    type.getExtendsList().add(nodeText(typeList.namedChild(i), source));
                }
            }
        }

        return type;
    }

    private List<FieldDeclaration> extractFields(TreeSitterNode classBody, String source) {
        List<FieldDeclaration> fields = new ArrayList<>();

        for (int i = 0; i < classBody.namedChildCount(); i++) {
            TreeSitterNode member = classBody.namedChild(i);
            if (!"field_declaration".equals(member.type())) {
                continue;
            }

            FieldDeclaration field = new FieldDeclaration();

            TreeSitterNode modifiers = findChild(member, "modifiers");
            field.setAnnotations(extractAnnotations(modifiers, source));
            field.setType(extractFieldType(member, source));

            List<String> variables = new ArrayList<>();
            for (int j = 0; j < member.namedChildCount(); j++) {
                TreeSitterNode child = member.namedChild(j);
                if ("variable_declarator".equals(child.type())) {
                    String varName = extractIdentifier(child, source);
                    if (varName != null) {
                        variables.add(varName);
                    }
                }
            }
            field.setVariables(variables);
            fields.add(field);
        }

        return fields;
    }

    private List<String> extractAnnotations(TreeSitterNode modifiers, String source) {
        List<String> annotations = new ArrayList<>();
        if (modifiers == null) {
            return annotations;
        }

        for (int i = 0; i < modifiers.namedChildCount(); i++) {
            TreeSitterNode child = modifiers.namedChild(i);
            if ("marker_annotation".equals(child.type()) || "annotation".equals(child.type())) {
                annotations.add(nodeText(child, source));
            }
        }
        return annotations;
    }

    private String extractFieldType(TreeSitterNode fieldDecl, String source) {
        for (int i = 0; i < fieldDecl.namedChildCount(); i++) {
            TreeSitterNode child = fieldDecl.namedChild(i);
            String type = child.type();
            if (!"modifiers".equals(type) && !"variable_declarator".equals(type)) {
                return nodeText(child, source);
            }
        }
        return null;
    }

    private String extractPackageName(TreeSitterNode packageDecl, String source) {
        for (int i = 0; i < packageDecl.namedChildCount(); i++) {
            TreeSitterNode child = packageDecl.namedChild(i);
            if ("scoped_identifier".equals(child.type()) || "identifier".equals(child.type())) {
                return nodeText(child, source);
            }
        }
        return "";
    }

    private String extractImportPath(TreeSitterNode importDecl, String source) {
        for (int i = 0; i < importDecl.namedChildCount(); i++) {
            TreeSitterNode child = importDecl.namedChild(i);
            if ("scoped_identifier".equals(child.type()) || "identifier".equals(child.type())) {
                return nodeText(child, source);
            }
        }
        return "";
    }

    private String extractIdentifier(TreeSitterNode node, String source) {
        for (int i = 0; i < node.namedChildCount(); i++) {
            TreeSitterNode child = node.namedChild(i);
            if ("identifier".equals(child.type())) {
                return nodeText(child, source);
            }
        }
        return null;
    }

    private TreeSitterNode findChild(TreeSitterNode node, String type) {
        for (int i = 0; i < node.namedChildCount(); i++) {
            TreeSitterNode child = node.namedChild(i);
            if (child.type().equals(type)) {
                return child;
            }
        }
        return null;
    }

    private String nodeText(TreeSitterNode node, String source) {
        if (node == null) {
            return null;
        }
        return source.substring(node.startByte(), node.endByte());
    }

    private String qualifiedName(String packageName, String simpleName) {
        if (packageName == null || packageName.isEmpty()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    private List<Path> findJavaFiles(Path projectPath) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        for (Path part : projectPath.relativize(p)) {
                            if (EXCLUDED_DIRS.contains(part.toString())) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .forEach(files::add);
        } catch (IOException e) {
            logger.errorf("Error walking project tree: %s", e.getMessage());
        }
        return files;
    }
}