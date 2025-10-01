package org.openrewrite.quarkus.spring;

import dev.snowdrop.openrewrite.recipe.spring.AddQuarkusRun;
import dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RemoveMethodInvocations;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceSpringBootApplicationAnnotationTest implements RewriteTest {

    @Test
    void ShouldReplaceAnnotationAndMain() {
        rewriteRun(spec -> spec.recipes(
                new ReplaceSpringBootApplicationWithQuarkusMainAnnotation(),
                new RemoveMethodInvocations("org.springframework.boot.SpringApplication run(..)"),
                new AddQuarkusRun()
            )
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
            java(
                """
                    package com.todo.app;
                    
                    import org.springframework.boot.SpringApplication;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;
                    
                    @SpringBootApplication
                    public class AppApplication {
                       public static void main(String[] args) {
                           SpringApplication.run(AppApplication.class, args);
                       }
                    }
                    """,
                """
                    package com.todo.app;
                    
                    import io.quarkus.runtime.Quarkus;
                    import io.quarkus.runtime.annotations.QuarkusMain;
                    
                    @QuarkusMain
                    public class AppApplication {
                       public static void main(String[] args) {
                           Quarkus.run(args);
                       }
                    }
                    """
            )
        );
    }

    /*
     Replace the @SpringBootApplication annotation with @QuarkusMain
     using a rewrite yaml file having as definition:
     preconditions:
       - org.openrewrite.maven.search.ParentPomInsight:
           groupIdPattern: org.springframework.boot
           artifactIdPattern: spring-boot-starter-parent
           version: 3.x
     recipeList:
       - dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationAnnotationWithQuarkusMain
    */
    @Test
    void shouldReplaceClassAnnotationUsingYamlRecipe() {
        rewriteRun(
            spec -> spec.recipeFromResource("/META-INF/rewrite/spring-boot-to-quarkus.yml", "dev.snowdrop.openrewrite.recipe.spring.ReplaceSpringBootApplicationWithQuarkusMainAnnotation")
                .parser((Parser.Builder) JavaParser.fromJavaVersion()
                    .classpath("spring-context", "spring-boot")
                    .logCompilationWarningsAndErrors(true)),
            java(
                // The Java source file before the recipe is run:
                """
                    package com.todo.app;
                    
                    import org.springframework.boot.SpringApplication;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;
                    
                    @SpringBootApplication
                     public class AppApplication {
                     	public static void main(String[] args) {
                             SpringApplication.run(AppApplication.class, args);
                     	}
                    }
                    """,
                // The expected Java source file after the recipe is run:
                """
                    package com.todo.app;
                    
                    import io.quarkus.runtime.annotations.QuarkusMain;
                    import org.springframework.boot.SpringApplication;
                    
                    @QuarkusMain
                     public class AppApplication {
                     	public static void main(String[] args) {
                             SpringApplication.run(AppApplication.class, args);
                     	}
                    }
                    """
            )
        );
    }

    /*@Test
    void shouldNotReplaceAnnotationAndRunMethod() {
        rewriteRun(
            java(
                // The Java source file before the recipe is run:
                """
                package com.todo.app.service;
                
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.data.domain.Page;
                import org.springframework.data.domain.PageRequest;
                import org.springframework.data.domain.Pageable;
                import org.springframework.stereotype.Service;
                import jakarta.persistence.*;
                import org.springframework.format.annotation.DateTimeFormat;
                
                import java.util.List;
                
                @Service
                public class TaskServiceImpl {
               
                  public Page<Task> getAllTasksPage(int pageNo, int pageSize) {
                    Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
                    return taskRepository.findAll(pageable);
                  }
                
                }
                public interface TaskRepository extends JpaRepository<Task, Long> {}
                
                @Entity
                @Table(name = "tasks")
                class Task {
                
                  @Id
                  @GeneratedValue(strategy = GenerationType.AUTO)
                  private Long id;
                
                  private String title;
                
                  private String description;
                
                  @DateTimeFormat(pattern = "yyyy-MM-dd")
                  private LocalDate dueDate;
                
                  public Task() {
                  }
                
                  public Task(Long id, String title, String description, LocalDate dueDate) {
                    this.id = id;
                    this.title = title;
                    this.description = description;
                    this.dueDate = dueDate;
                  }
                
                  public Long getId() {
                    return id;
                  }
                
                  public void setId(Long id) {
                    this.id = id;
                  }
                
                  public String getTitle() {
                    return title;
                  }
                
                  public String getDescription() {
                    return description;
                  }
                
                  public LocalDate getDueDate() {
                    return dueDate;
                  }
                
                  public void setTitle(String title) {
                    this.title = title;
                  }
                
                  public void setDescription(String description) {
                    this.description = description;
                  }
                
                  public void setDueDate(LocalDate dueDate) {
                    this.dueDate = dueDate;
                  }
                }
                """,
                // The expected Java source file after the recipe is run:
                """
                package com.todo.app.service;
                
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.data.domain.Page;
                import org.springframework.data.domain.PageRequest;
                import org.springframework.data.domain.Pageable;
                import org.springframework.stereotype.Service;
                import jakarta.persistence.*;
                import org.springframework.format.annotation.DateTimeFormat;
                import java.util.List;
                
                @Service
                public class TaskServiceImpl {
               
                  public Page<Task> getAllTasksPage(int pageNo, int pageSize) {
                    Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
                    return taskRepository.findAll(pageable);
                  }
             
                  interface TaskRepository extends JpaRepository<Task, Long> {}
                
                  @Entity
                  @Table(name = "tasks")
                  class Task {
                  
                    @Id
                    @GeneratedValue(strategy = GenerationType.AUTO)
                    private Long id;
                  
                    private String title;
                  
                    private String description;
                  
                    @DateTimeFormat(pattern = "yyyy-MM-dd")
                    private LocalDate dueDate;
                  
                    public Task() {
                    }
                  
                    public Task(Long id, String title, String description, LocalDate dueDate) {
                      this.id = id;
                      this.title = title;
                      this.description = description;
                      this.dueDate = dueDate;
                    }
                  
                    public Long getId() {
                      return id;
                    }
                  
                    public void setId(Long id) {
                      this.id = id;
                    }
                  
                    public String getTitle() {
                      return title;
                    }
                  
                    public String getDescription() {
                      return description;
                    }
                  
                    public LocalDate getDueDate() {
                      return dueDate;
                    }
                  
                    public void setTitle(String title) {
                      this.title = title;
                    }
                  
                    public void setDescription(String description) {
                      this.description = description;
                    }
                  
                    public void setDueDate(LocalDate dueDate) {
                      this.dueDate = dueDate;
                    }
                  }
                }  
                """
            )
        );
    }*/
}
