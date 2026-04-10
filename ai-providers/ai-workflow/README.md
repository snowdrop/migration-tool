## AI Workflow

This project explores how we could use AI and non AI agents part of an Agentic Workflow. 
TODO

Before to decide what we will do it is also important to answer to the following question as most of the tasks to be executed part of a plan ciuld also be managed by
AI directly (= workflow processor) using Tools

### Should we create an AI workflow ?

I posted to Claude the following question:
"How can we programmatically use langchain4j perform what you did using the tasks_plan, process through the steps, cross check a box when completed, etc ? "

and it replied

Here's the design, grounded in the patterns you already use:
```yaml                                                                                                                                                                                             
---                                                                                                                                                                                        
Architecture Overview

MigrationPlanAgent (orchestrator)                                                                                                                                                          
│                                                                                                                                                                                        
├─ MigrationTools (@Tool methods the AI can call)                                                                                                                                        
│    ├─ readMigrationPlan(filePath)
│    ├─ markTaskCompleted(filePath, taskText)                                                                                                                                            
│    ├─ readSourceFile(path)                                                                                                                                                             
│    ├─ writeFile(path, content)
│    ├─ updateProperty(key, value, profile)
│    ├─ modifyPomDependency(action, groupId, artifactId)
│    ├─ executeMaven(args)
│    └─ applyOpenRewriteRecipe(recipeName)
│
└─ ChatMemory (MessageWindowChatMemory)
```

The LLM acts as the reasoning loop -- it reads the plan, decides which tool to call for each unchecked task, calls it, verifies the outcome, and marks the checkbox. No routing agents
needed; the tool-calling loop handles it.

  ---
1. The Migration Tools (like your MigrationActions)

```java
package dev.snowdrop.ai.migration;

import dev.langchain4j.agent.tool.Tool;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;

public class MigrationTools {

      private final Path projectRoot;

      public MigrationTools(Path projectRoot) {
          this.projectRoot = projectRoot;
      }

      // ── Plan management ──────────────────────────────────

      @Tool("Read the migration plan markdown file and return its content")
      public String readMigrationPlan(String filePath) throws IOException {
          return Files.readString(projectRoot.resolve(filePath));
      }

      @Tool("Mark a specific task as completed in the markdown plan. "
          + "Replaces '- [ ]' with '- [x]' for the line matching the task text.")
      public String markTaskCompleted(String filePath, String taskText)
              throws IOException {
          Path path = projectRoot.resolve(filePath);
          String content = Files.readString(path);

          // Escape regex chars in taskText, match the unchecked box line
          String escaped = Pattern.quote(taskText.trim());
          String updated = content.replaceFirst(
              "- \\[ \\] " + escaped,
              "- [x] " + escaped
          );

          if (updated.equals(content)) {
              return "Task not found or already completed: " + taskText;
          }
          Files.writeString(path, updated);
          return "Marked as completed: " + taskText;
      }

      // ── File operations ──────────────────────────────────

      @Tool("Read a source file from the project and return its content")
      public String readSourceFile(String relativePath) throws IOException {
          return Files.readString(projectRoot.resolve(relativePath));
      }

      @Tool("Write content to a file in the project, creating it if needed")
      public String writeFile(String relativePath, String content)
              throws IOException {
          Path path = projectRoot.resolve(relativePath);
          Files.createDirectories(path.getParent());
          Files.writeString(path, content);
          return "Written: " + relativePath;
      }

      @Tool("Delete a file from the project")
      public String deleteFile(String relativePath) throws IOException {
          Files.deleteIfExists(projectRoot.resolve(relativePath));
          return "Deleted: " + relativePath;
      }

      // ── application.properties management ────────────────

      @Tool("Set or update a property in application.properties. "
          + "Use profile prefix like '%prod.' for profile-specific properties.")
      public String updateProperty(String key, String value) throws IOException {
          Path propsPath = projectRoot.resolve(
              "src/main/resources/application.properties");
          String content = Files.readString(propsPath);

          String escapedKey = Pattern.quote(key);
          if (content.matches("(?s).*^" + escapedKey + "=.*$.*")) {
              content = content.replaceAll(
                  "(?m)^" + escapedKey + "=.*$",
                  key + "=" + value
              );
          } else {
              content += "\n" + key + "=" + value;
          }

          Files.writeString(propsPath, content);
          return "Property set: " + key + "=" + value;
      }

      // ── Maven operations ─────────────────────────────────

      @Tool("Execute a Maven command in the project directory "
          + "and return stdout (first 30 lines) plus exit code")
      public String executeMaven(String args) throws Exception {
          ProcessBuilder pb = new ProcessBuilder();
          pb.command("bash", "-c", "./mvnw " + args);
          pb.directory(projectRoot.toFile());
          pb.redirectErrorStream(true);
          Process p = pb.start();
          String output = new String(p.getInputStream().readAllBytes());
          int exit = p.waitFor();

          String[] lines = output.split("\n");
          StringBuilder result = new StringBuilder();
          for (int i = 0; i < Math.min(30, lines.length); i++) {
              result.append(lines[i]).append("\n");
          }
          result.append("\n--- Exit code: ").append(exit).append(" ---");
          return result.toString();
      }

      @Tool("Execute an OpenRewrite recipe via Maven")
      public String applyOpenRewriteRecipe(String recipeName) throws Exception {
          return executeMaven(
              "rewrite:run -Drewrite.activeRecipes=" + recipeName);
      }
}
```

This follows the same pattern as your existing `MigrationActions class` in `JBangMigrationAiTool.java`, but with tools specific to the migration plan workflow.

---
2. The Migration Agent (AI Service)

```java
package dev.snowdrop.ai.migration;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = MigrationTools.class)
public interface MigrationAgent {

      @SystemMessage("""
          You are a Spring Boot to Quarkus migration agent.

          Your workflow:
          1. Read the migration plan using readMigrationPlan("tasks_plan.md")
          2. Find the FIRST unchecked task (a line with "- [ ]")
          3. Determine what file changes are needed for that task
          4. Read the relevant source file(s) using readSourceFile()
          5. Apply the change using writeFile(), updateProperty(),
             or executeMaven() as appropriate
          6. Verify the change succeeded (re-read the file or run mvn compile)
          7. Mark the task completed using markTaskCompleted()
          8. Move to the next unchecked task
          9. Stop when all tasks in the current phase are checked

          Rules:
          - Always read a file before modifying it
          - After writing code changes, verify with executeMaven("compile")
          - Only mark a task completed after verification passes
          - If a step fails, report the error and do NOT mark it completed
          - Process ONE task at a time
          """)
      String processNextPhase(@UserMessage @V("request") String request);
}
```
---
3. Running It (Picocli command, like your AiWorkFlowCommand)

```java
package dev.snowdrop.ai.migration;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;

@Command(name = "migrate", description = "Run AI-driven migration")
public class MigrationCommand implements Runnable {

      @Inject
      MigrationAgent migrationAgent;

      @Override
      public void run() {
          // Process the full plan phase by phase
          String result = migrationAgent.processNextPhase("""
              Read tasks_plan.md and process all unchecked tasks
              in Phase 1 (Build System). For each unchecked task,
              apply the change to the project files, verify it compiles,
              and mark the checkbox.
              """);

          System.out.println(result);
      }
}

Or to do it phase by phase in a loop:

@Override
public void run() {
String[] phases = {
"Phase 1: Build System (pom.xml)",
"Phase 2: Configuration (application.properties)",
"Phase 3: Entity Layer",
// ...
"Phase 11: Verification & Cleanup"
};

      for (String phase : phases) {
          System.out.println("=== Processing: " + phase + " ===");
          String result = migrationAgent.processNextPhase(
              "Process all unchecked tasks in: " + phase);
          System.out.println(result);
      }
}
```
  ---
4. Alternative: Using @SequenceAgent (your existing pattern)

If you want a more declarative approach like your StoryCreatorFlow, you could decompose into specialized agents per phase:
```java
// Router decides which phase agent to invoke
@Agent(description = "Routes migration task to the correct phase handler")
public interface MigrationRouter {
@UserMessage("""
Given this task from the migration plan: {task}
Categorize it as one of: POM, CONFIG, ENTITY, REPOSITORY,
CONTROLLER, TEMPLATE, TEST, VERIFY
""")
@Agent(outputKey = "category")
String categorize(@V("task") String task);
}

// Phase-specific agent
@RegisterAiService(tools = MigrationTools.class)
public interface PomMigrationAgent {
@SystemMessage("You modify pom.xml for Spring Boot to Quarkus migration.")
@Agent(description = "Handles pom.xml dependency changes",
outputKey = "result")
String process(@V("task") String task);
}
```
But the tool-driven single-agent approach (option 2 above) is simpler and more practical here, because the LLM can reason about which tools to call based on the task description. Adding
routing agents would be useful if you want non-AI phases (like your NonAiAgentCreativeWriter pattern) -- for example, OpenRewrite recipe execution doesn't need AI reasoning, just a direct
tool call.

---
Key Design Decisions
┌──────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────┐
│     Concern      │                                             Approach                                             │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Plan parsing     │ Let the LLM read the markdown directly -- it understands checkbox syntax natively                │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Checkbox updates │ @Tool method with regex - [ ] → - [x] replacement                                                │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Verification     │ After each change, call executeMaven("compile") before marking done                              │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Error handling   │ System prompt instructs the agent NOT to check the box if verification fails                     │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Scope control    │ Process one phase at a time to stay within context window limits                                 │
├──────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Memory           │ MessageWindowChatMemory (like your JBangMigrationAiTool) keeps recent tool call/response context │
└──────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────┘
The main insight is that your tasks_plan.md is already a structured enough format for the LLM to parse directly -- no need for a separate plan parser. The LLM reads the markdown, finds
unchecked items, reasons about what tools to call, and updates the checkboxes. The @Tool methods are the hands; the LLM is the brain.