# Smart Language Learner Platform — Backend

Spring Boot 4 microservices backend for the SLLP application.

The Product Guide Service adds authenticated
application-help chat with streaming responses and per-user PostgreSQL conversation memory.
It retrieves platform feature information from pgvector before answering and is available
through the API gateway when the main Docker Compose stack is running.

## Prerequisites

| Tool | Version |
|---|---|
| Docker Desktop | 4.x+ |
| Docker Compose | v2 (bundled with Docker Desktop) |

> **Note:** No local JDK or Maven installation is required. All services are compiled inside Docker using multi-stage builds.

---

## Quick start

The Product Guide Service requires `OPENAI_API_KEY`. Keep it in the ignored `.env` file
or export it in your shell before running Docker Compose.

### Local audit configuration

Audit ingestion is optional for local development. Leave these values blank to start the
platform without cross-service audit ingestion. Set both to strong, distinct secrets when
you want to enable it (and always in non-local environments):

```bash
AUDIT_INGEST_SECRET=<high-entropy HMAC secret>
AUDIT_DB_PASSWORD=<strong database password>
```

`AUDIT_INGEST_SECRET` is for backend services only. Do not expose either value through a `NEXT_PUBLIC_*` frontend variable.

### First run (build images from source)

```bash
docker compose up --build
```

This will:
1. Build each service JAR inside Docker (no pre-built JARs needed)
2. Start PostgreSQL and wait for it to be healthy
3. Run all Liquibase schema migrations
4. Start all application services once migrations complete

### Subsequent runs (images already built)

```bash
docker compose up
```

### Detached (background) mode

```bash
docker compose up --build -d   # first run
docker compose up -d           # subsequent runs
```

### Rebuild a single service

```bash
docker compose up --build -d <service-name>
# e.g.
docker compose up --build -d user-service
docker compose up --build -d document-service
```

---

## Reset (wipe all data)

```bash
docker compose down -v
```

The `-v` flag removes named volumes, wiping the PostgreSQL data directory. The next `docker compose up --build` will start completely fresh.

---

## Services and ports

| Service | Container | Port | Notes |
|---|---|---|---|
| PostgreSQL 18 | `postgres-db` | `5432` | Persistent volume `sllp-data` |
| Liquibase | `liquibase-service` | — | Runs migrations then exits |
| User Service | `user-service` | `8082` | Auth, registration |
| Composition Service | `composition-service` | `8083` | Composition CRUD + Kafka |
| Document Service | `document-service` | `8089` | File upload, text extraction |
| Product Guide Service | `product-guide-service` | `8087` (internal) | Streaming AI help grounded by pgvector |
| API Gateway | `api-gateway` | `8080` | Single entry point |
| Kafka | `kafka-service` | `9092` / `29092` | |
| LangGraph Orchestrator | `orchestrator-service` | `8090` | Kafka workflow coordinator for assessment agents |
| MinIO (S3-compatible) | `minio` | `9000` / `9001` | Console at `:9001` |

The Product Guide Service indexes its feature catalogue with the configured OpenAI embedding model
whenever the service starts. Existing entries are updated using their stable identifiers.

> **Disabled service:** The previous single-call `evaluation-service` remains commented out in `docker-compose.yaml`. The LangGraph orchestrator is the only grading-topic consumer. By default it uses a clearly labelled, contract-valid development placeholder while the team-owned agents are being built.

When `AGENT_PROVIDER=a2a` is enabled later, the orchestrator verifies each peer's Agent Card before delegation and uses its advertised A2A endpoint for `SendMessage`. Agent owners should follow [the SLLP A2A agent contract](sllp-orchestrator-service/AGENT_CONTRACT.md), including `/.well-known/agent-card.json` discovery.

The default `AGENT_PROVIDER=placeholder` exercises Kafka, workflow persistence, completion publication, and the existing frontend flow without an LLM. Its score and suggestions are explicitly labelled as development placeholders and must not be used as a real assessment.

Set `AGENT_PROVIDER=a2a` when the team-owned peers are ready. The A2A provider rejects malformed, low-confidence, or unreviewed assessments.

---

## How it works

### Multi-stage Docker builds

Every service Dockerfile has two stages:

1. **Builder stage** — installs Maven on Alpine JDK 25, copies the parent `pom.xml` and service `pom.xml` (layer-cached), downloads dependencies, then compiles and packages the JAR with `mvn package -DskipTests`.
2. **Runtime stage** — copies only the finished JAR into a clean Alpine JDK 25 image.

This means you never need to run `mvn package` locally before `docker compose build`.

### Startup ordering

```
postgres-db (healthy?) → liquibase-service (completed?) → user-service / document-service / api-gateway
```

- `postgres-db` exposes a `pg_isready` healthcheck so downstream services don't start until Postgres accepts connections.
- `liquibase-service` runs all Liquibase migrations and exits with code 0. Services that depend on it use `condition: service_completed_successfully` so they wait until every migration has applied.

### Database schema

Schema is managed entirely by Liquibase changelogs under `sllp-liquibase-service/src/main/resources/changelog/schema/`:

| File | Description |
|---|---|
| `001-schema-init.sql` | Creates all tables and seeds dev users |
| `002-schema-improvements.sql` | Migrates `username` → `user_id`, normalises subscriptions, adds `evaluation` table, renames token columns |

To add a schema change, **add a new changeset** at the end of an existing file or create a new numbered file (e.g. `003-your-feature.sql`). Never edit an existing changeset — Liquibase validates checksums on every startup and will refuse to run if a recorded changeset has been modified.

> **Rule for all developers**: Changesets are immutable once committed. Edit = breaking change for every other developer's database. Always append, never modify.

The `001-schema-init.sql` file is also mounted into Postgres's `docker-entrypoint-initdb.d/` so the schema is available immediately on first start, before the Liquibase container runs.

---

## Seed users (dev only)

| Username | Password | Role |
|---|---|---|
| `admin` | `password123` | `ADMIN` |
| `testuser` | `password123` | `USER_FREE_TRIAL` |

---

## MinIO (object storage)

MinIO runs as an S3-compatible store for document uploads. Credentials:

| Key | Value |
|---|---|
| Access key | `minioadmin` |
| Secret key | `minioadmin123` |
| Console | http://localhost:9001 |

Override with environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.

---

## Troubleshooting

### Liquibase checksum error on restart (without `-v`)

If you modify a Liquibase changeset that was already applied, Liquibase will refuse to start with a checksum mismatch. Either:
- Do a full reset: `docker compose down -v && docker compose up --build`
- Or clear the Liquibase tracking table manually:
  ```bash
  docker compose exec postgres-db psql -U postgres -d sllp_main_db \
    -c "DELETE FROM databasechangelog WHERE id = '<changeset-id>'"
  ```

### Services start before migrations finish

This is prevented by the `condition: service_completed_successfully` guard on `liquibase-service`. If you see column-not-found errors, verify Liquibase exited with code 0:
```bash
docker compose logs liquibase-service
```
