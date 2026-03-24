# java-orm-benchmark

Real-world benchmarks for Java persistence frameworks and ORMs across PostgreSQL and MySQL — performance, overhead, and working code in one place.

## Project Structure

```
java-orm-benchmark/
├── common/                  Shared: domain model, config, seeding, result models
├── benchmark-harness/       Central JMH runner with ORM adapter pattern
├── docker/                  Docker Compose for databases + benchmark runner
├── scripts/                 Automation: run benchmarks, upload results, generate reports
├── results/                 Benchmark output (gitignored)
├── .github/workflows/       CI (build/test) + Benchmark (manual dispatch)
└── gradle/libs.versions.toml   Single source of truth for all versions
```

## Prerequisites

- Java 25 (or the version specified in `gradle/libs.versions.toml`)
- Docker and Docker Compose
- `jq` (for scripts)

## Quick Start

### Start databases
```bash
docker compose -f docker/docker-compose.yml up -d
```

### Build
```bash
./gradlew clean build
```

### Run benchmarks locally
```bash
./scripts/run-benchmarks.sh          # Full run
./scripts/run-benchmarks.sh --quick  # Reduced iterations
```

### Upload results to Gist
```bash
GIST_TOKEN=<your-token> GIST_ID=<gist-id> ./scripts/upload-gist.sh results/latest.json
```

### Docker-based benchmark (isolated environment)
```bash
./gradlew clean shadowJar
docker compose -f docker/docker-compose.benchmark.yml up --build
```

## Version Management

All versions (Java, databases, libraries, plugins) are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). Update a version in one place; it propagates everywhere.

## Adding a New ORM Module

1. Create `orm-<name>/` directory with a `build.gradle`
2. Add `include 'orm-<name>'` to `settings.gradle`
3. Add version and library entries to `gradle/libs.versions.toml`
4. Add as dependency in `benchmark-harness/build.gradle`
5. Implement the `OrmAdapter` interface in `benchmark-harness`

## CI / Benchmarks

- **CI** (`ci.yml`): Runs on every push/PR — builds and tests
- **Benchmark** (`benchmark.yml`): Manual dispatch via GitHub Actions — runs full JMH suite, uploads results to [Gist](https://gist.github.com/LGalabov/0c660c130278e8a7b39fc702d8693f4e)

## License

See [LICENSE](LICENSE).
