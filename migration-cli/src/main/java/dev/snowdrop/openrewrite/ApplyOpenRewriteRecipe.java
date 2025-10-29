package dev.snowdrop.openrewrite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.snowdrop.openrewrite.model.Result;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation;
import org.eclipse.lsp4j.SymbolKind;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ApplyOpenRewriteRecipe {

    private static Instant start;
    private static Instant finish;

    public static void main(String[] args) {

        String javaFilePath = Optional.ofNullable(System.getProperty("JAVA_SPRINGBOOT_APPLICATION_FILE_PATH")).orElse(
                "/Users/cmoullia/code/application-modernisation/spring-to-quarkus-guide/java-analyzer-quarkus/applications/spring-boot-todo-app/src/main/java/com/todo/app/AppApplication.java");

        String JDT_LS_JSON_RESPONSE = String.format("""
                [
                  {
                    "name": "org.springframework.boot.autoconfigure.SpringBootApplication",
                    "kind": 5,
                    "location": {
                      "uri": "file://%s",
                      "range": {
                        "start": { "line": 3.0, "character": 7.0 },
                        "end": { "line": 3.0, "character": 67.0 }
                      }
                    },
                    "containerName": ""
                  }
                ]""", javaFilePath);

        start = Instant.now();

        // Configure Jackson ObjectMapper with custom SymbolKind deserializer
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SymbolKind.class, new SymbolKindDeserializer());
        objectMapper.registerModule(module);

        // Parse JSON using Jackson
        TypeReference<List<Result>> resultListType = new TypeReference<List<Result>>() {
        };
        List<Result> results;
        try {
            results = objectMapper.readValue(JDT_LS_JSON_RESPONSE, resultListType);
        } catch (Exception e) {
            System.err.println("Failed to parse jdt language server JSON response: " + e.getMessage());
            return;
        }

        if (results.isEmpty()) {
            System.out.println("No result found !!");
            return;
        }

        Result target = results.get(0);
        System.out.println(
                "Found symbol '" + target.name() + "' with kind: " + target.kind() + " within the json string");

        // Apply the Openrewrite transformation
        runOpenRewriteTransformation(target);
    }

    private static void runOpenRewriteTransformation(Result result) {
        String sourceCode;
        try {
            URI fileUri = new URI(result.location().uri());
            Path filePath = Paths.get(fileUri);

            // Read the content from the file
            sourceCode = Files.readString(filePath);
            System.out.println("\nSuccessfully read content from: " + filePath);

        } catch (Exception e) {
            System.err.println("Failed to read or create source file from URI: " + result.location().uri());
            e.printStackTrace();
            return; // Exit if we can't get the source code
        }

        System.out.println("--- SOURCE CODE BEFORE TRANSFORMATION ---");
        System.out.println(sourceCode);
        System.out.println("-----------------------------------------\n");

        Recipe recipe = new ReplaceSpringBootApplicationWithQuarkusMainAnnotation();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        // Parse the source file into OpenRewrite's format (LST)
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse(ctx, sourceCode).findFirst()
                .map(J.CompilationUnit.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse source code."));

        J.CompilationUnit transformedCu = (J.CompilationUnit) recipe.getVisitor().visit(cu, ctx);

        if (transformedCu == cu) {
            System.out.println("Recipe did not make any changes.");
        } else {
            String transformedCode = transformedCu.printAll();
            System.out.println("--- SOURCE CODE AFTER TRANSFORMATION ---");
            System.out.println(transformedCode);
            System.out.println("----------------------------------------");
        }

        finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        System.out.println("--- Elapsed time: " + timeElapsed + " ms ---");

    }
}
