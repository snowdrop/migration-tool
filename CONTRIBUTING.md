# Contributing to Migration Tool

Thank you for your interest in contributing to the Migration Tool project! This guide will help you get started.

## Prerequisites

- Java 21
- Maven 3.9+
- Git

## Getting started

1. Fork and clone the repository:
```shell
git clone https://github.com/<your-username>/migration-tool.git
cd migration-tool
```

2. Build the project:
```shell
mvn clean install -DskipTests
```

3. Run the tests:
```shell
mvn test
```

## Project structure

| Module | Description |
|--------|-------------|
| `model` | Data model for rules, analysis reports, and configuration |
| `parser` | Antlr grammar and generated code for the query language |
| `scanner` | Pluggable scanners (OpenRewrite, Tree-sitter, JavaParser, Maven, file search) |
| `service` | Validators (project-setup, persistence) and result services |
| `mcp` | Quarkus MCP server exposing tools to AI agents |
| `migration-cli` | Quarkus CLI application with [Aesh](https://aeshell.github.io/docs/aesh/) commands |
| `openrewrite-recipes` | Custom OpenRewrite recipes for code transformation |
| `tests` | Integration and end-to-end tests |

## How to contribute

### Reporting issues

- Use [GitHub Issues](https://github.com/snowdrop/migration-tool/issues) to report bugs or request features
- Include steps to reproduce, expected behavior, and actual behavior
- Mention the version you are using (`mtool --version`)

### Submitting changes

1. Create a branch from `main`:
```shell
git checkout -b feature/my-change
```

2. Make your changes and ensure:
   - The project compiles: `mvn clean install -DskipTests`
   - Tests pass: `mvn test`
   - Code is formatted: `mvn formatter:format`

3. Commit with a clear message describing the change

4. Push your branch and open a pull request against `main`

### Adding a new scanner

Implement the `QueryScanner` SPI in the `scanner` module and register it. See existing implementations (e.g. `TreeSitterQueryScanner`) for reference.

### Adding a new validator

Implement the `Validator` interface in the `service` module. Validators are discovered automatically from the classpath. See `PersistenceValidator` or `ProjectSetupValidator` for examples.

### Adding OpenRewrite recipes

Add new recipes to the `openrewrite-recipes` module. Follow the [OpenRewrite authoring guide](https://docs.openrewrite.org/authoring-recipes) and include tests using `RewriteTest`.

## Code style

The project uses the Quarkus Eclipse formatter configuration. Run `mvn formatter:format` before committing to ensure consistent formatting.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
