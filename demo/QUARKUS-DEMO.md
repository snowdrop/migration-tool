## Demo scenario

- Introduce speakers and purpose of the talk: Migration of applications
- Agenda
- OpenRewrite: Migration Swiss knife toolbox
    - Huge collection of recipes = java classes executed through visitor pattern
    - Can be used to transform OR analyze
    - Support "dryrun" or "run" mode
    - Executed using "maven"
      BUT
    - Slow when several "mvn" cmds should be executed,
    - No easy way to design: unit test
    - No API or Library
- Quarkus Rewrite Client
    - Client and Library designed around OpenRewrite
    - Bypass most of the limitations described previously
    - To use it, compile it locally or install it using jbang
      ```bash
      mvn clean install -DskipTests -Dquarkus.package.type=uber-jar
      jbang app install --force --catalog jbang-catalog-dev.json openrewrite
      ```
    - Demo 1
      ```bash
      pushd test-project/simple
      openrewrite . -r org.openrewrite.java.format.AutoFormat
      openrewrite . -r org.openrewrite.java.format.AutoFormat -d false
      openrewrite . -c rewrite.yml
      popd

      or

      openrewrite test-project/simple -c rewrite.yml
      ```
    - Demo 2
      ```bash
      Show some Test cases
      ```
- What about Migrating Applications
    - OpenRewrite tool => perfect to execute YAML recipes
    - Scenario could be really complex even when want to migrate some REST annotations
    - Demo 3
  ```bash
  pushd rewrite-client/test-project/quarkus-resteasy-classic-app
  openrewrite . -c rewrite.yml
  ```
    - YAML format is quite verbose, users will make errors, etc
    - Users should be educated, trained and ideally supported by a migration tool

- Present the `mtool` project
    - Quarkus Client able to analyze, generate a migration plan and transform
    - Demo 4
        - Present a migration scenario: Spring Boot Todo to Quarkus Todo
        - Show 1-2 rules => `preconditions` and `when condition`
        - Life becomes easier as Antlr can parse/process the `Query`
        - Run analysis and detail the console's table
          ```bash
          mtool analyze ./applications/spring-boot-todo-app -r ./cookbook/rules/quarkus-spring-precondition
          mtool analyze ./applications/spring-boot-todo-app -r ./cookbook/rules/quarkus-spring
          ```
          A report = migration plan has been generated packaging instructions :-)
        - Show that the SB Todo app is working
          ```bash
          mvn clean package spring-boot:run
          firefox localhost:8081
          ```
        - Do the transformation/migration using as provider "Openrewrite" => dryrun
          ```bash
          mtool transform ./applications/spring-boot-todo-app -p openrewrite --dry-run
          // Show patch files generated !
          mtool transform ./applications/spring-boot-todo-app -p openrewrite
          ``` 
            - Launch Quarkus Todo
          ```bash
          mvn clean package -DskipTests quarkus:dev
          firefox http://localhost:8080/q/dev-ui/extensions
          
          // Curl
          curl http://127.0.0.1:8080/home
          curl -X POST http://localhost:8080/home \
          -H "Content-Type: application/json" \
          -d '{
          "title": "Prepare migration to Quarkus. Task 1",
          "description": "Migrating Spring Boot TO DO to Quarkus. Task 1",
          "dueDate": "2025-12-24"
          }'
          
          curl -X POST http://localhost:8080/home \
          -H "Content-Type: application/json" \
          -d '{
          "title": "Prepare migration to Quarkus. Task 2",
          "description": "Migrating Spring Boot TO DO to Quarkus. Task 2",
          "dueDate": "2025-12-24"
          }'
          ```
    - What else: when we reach the limit of openrewrite or that no recipes => use AI
    - Demo 5
      ```bash
      run the video
      ```
- Wrap-up and Q/A