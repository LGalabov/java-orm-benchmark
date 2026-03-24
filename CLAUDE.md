# CLAUDE.md

## Git Conventions

- **Branch:** All commits go to `main` only. No feature branches, no merge requests. Push directly.
- **Commit format:** Conventional Commits. One-liner only. No bullet points, no body, no URLs, no promotional text.
  - Format: `<type>[optional scope]: <short description>`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`
  - Breaking changes: add `!` before colon, e.g. `feat!: remove legacy API`
  - Example: `feat: add project foundation`
  - Example: `fix: correct HikariCP version`
  - Example: `feat: add hibernate module`
  - Example: `ci: update Java version in workflows`
- **Clean history:** One commit per logical change. Do not pollute history with multiple commits for the same purpose. Squash if needed before pushing.
- **Push target:** `git push -u origin main`

## Build

- Gradle multi-module project. Run `./gradlew clean build` to compile and test.
- All versions centralized in `gradle/libs.versions.toml`.
- Shadow plugin (`com.gradleup.shadow`) is declared in the version catalog but commented out in `benchmark-harness/build.gradle` until benchmark code is added.

## Project Layout

- `common/` — shared module (domain model, config, seeding, result models)
- `benchmark-harness/` — central JMH benchmark runner with adapter pattern
- `docker/` — Docker Compose for PostgreSQL + MySQL, plus benchmark runner
- `scripts/` — shell scripts for running benchmarks, uploading results, etc.
- `.github/workflows/` — CI (push/PR) and Benchmark (manual dispatch)
- `gradle/libs.versions.toml` — single source of truth for all versions
