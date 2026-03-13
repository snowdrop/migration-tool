## Demo scenario

To present the slides, simply open the HTML revealjs file
```bash
cd demo
firefox quarkus-meeting-feb-2024.html
```

DEMO 0 - install rewrite client (Charles)
```bash
jbang app install --force rewrite@snowdrop/rewrite-client/0.3.0
```
Demo 1 - Autoformat a java project with or without dryrun (Charles)
```bash
pushd test-project/simple

mvn org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.activeRecipes=org.openrewrite.java.format.AutoFormat
  
rewrite . -r org.openrewrite.java.format.AutoFormat
rewrite . -r org.openrewrite.java.format.AutoFormat -d false
popd
```
Demo2 - Use Recipes YAML (Aurea)
```bash
pushd test-project/simple
rewrite . -c rewrite.yml
```
Demo 3 - A more complex migration (Aurea)
```bash
pushd rewrite-client/test-project/quarkus-resteasy-classic-app
rewrite . -c rewrite.yml
```

Demo 4 (Aurea)
```bash
mtool analyze ./applications/spring-boot-todo-app -r ./cookbook/rules/quarkus-spring-precondition
mtool analyze ./applications/spring-boot-todo-app -r ./cookbook/rules/quarkus-spring
```
A report = migration plan has been generated packaging instructions :-)
Show that the SB Todo app is working
```bash
mvn clean package spring-boot:run
firefox localhost:8081
```
Do the transformation/migration using as provider "Openrewrite" => dryrun
 ```bash
 mtool transform ./applications/spring-boot-todo-app -p openrewrite --dry-run
 // Show patch files generated !
 mtool transform ./applications/spring-boot-todo-app -p openrewrite
``` 
Launch Quarkus Todo
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
What else: when we reach the limit of openrewrite or that no recipes => use AI

Demo 5 - Charles
```bash
run the video
```