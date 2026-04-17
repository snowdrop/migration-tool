---
name: check-jdk
description: Checks that JDK 21 is installed and active before starting the migration. Warns the user and stops if the requirement is not met.
license: Apache 2
metadata:
  author: snowdrop
  version: "0.1.0"
---

# Phase 0: Check JDK Version

Verify that the installed JDK meets the minimum version requirement (JDK 21) before proceeding with the migration.

## Preconditions

This phase has no preconditions — it must **always** run as the very first step.

## Instructions

- **DO NOT** skip this phase.
- Check the JDK version.
- If the JDK version is less than 21, **warn the user** and **stop the migration**.

## Migration Steps

- [ ] Run `java -version` and parse the version number from the output
- [ ] If the version is **>= 21**, mark this phase as passed and proceed to the next phase
- [ ] If the version is **< 21** or `java` is not found:
  - Warn the user: "JDK 21 or later is required for this migration. Currently, installed: <detected version or 'none'>. Please install JDK 21+ and ensure it is on your PATH before retrying."
  - **Stop the migration** — do not proceed to any subsequent phase