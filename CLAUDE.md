# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

D7032E home exam. <!-- TODO: describe the exam task, requirements, and any rules (allowed libraries, required tests, design principles, etc.) -->

## Build & run

- Plain IntelliJ project: no Maven/Gradle. Sources live in `src/`, and IntelliJ compiles to `out/`.
- JDK 26. `src/Main.java` is a compact source file with an instance `void main()` and `IO.println`, so don't "fix" it into a classic `public static void main` class.
- To run from the CLI: `java src/Main.java`. This launches the source file directly, so no separate javac step is needed.
