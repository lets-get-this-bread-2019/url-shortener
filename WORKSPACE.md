# URL Shortener - Workspace Setup

**Last Updated**: 2026-03-22
**Engineer**: Founding Engineer (f4bafd02-62fe-4ad4-bfe2-0d00a4885ccd)
**Workspace**: `/Users/sahiljain/projects/url-shortener`

## Overview

This workspace contains a Java Spring Boot URL shortener service, managed through Paperclip task coordination. The Founding Engineer owns all implementation, testing, and deployment tasks.

## Workspace Access

✅ **Primary Workspace**: `/Users/sahiljain/projects/url-shortener`
✅ **Paperclip Codebase**: `/Users/sahiljain/paperclip` (accessible for reference)
✅ **Paperclip API**: `http://127.0.0.1:3100` (local instance)

## Project Structure

```
/Users/sahiljain/projects/url-shortener/
├── src/
│   ├── main/java/com/example/urlshortener/
│   │   ├── UrlShortenerApplication.java    # Main Spring Boot app
│   │   ├── controller/
│   │   │   ├── UrlController.java          # /shorten endpoint
│   │   │   └── HomeController.java         # / root endpoint
│   │   ├── service/UrlService.java         # Business logic
│   │   ├── repository/UrlRepository.java   # Data access
│   │   └── model/ShortUrl.java             # Domain model
│   ├── main/resources/
│   │   ├── application.properties          # Spring config
│   │   └── schema.sql                      # DB schema
│   └── test/java/com/example/urlshortener/
│       └── UrlControllerTest.java          # Controller tests
├── pom.xml                                  # Maven dependencies
├── AGENTS.md                                # Agent instructions
├── WORKSPACE.md                             # This file
├── Dockerfile                               # Container config
├── render.yaml                              # Render deployment config
└── url-shortener.db                         # Local SQLite (legacy)
```

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.3
- **Build Tool**: Maven
- **Database**:
  - **Production**: PostgreSQL (via Render)
  - **Local Dev**: PostgreSQL (via `DATABASE_URL` env var)
  - **Tests**: H2 (in-memory)
  - **Legacy**: SQLite (superseded by PostgreSQL migration)
- **Testing**: JUnit 5 + Spring MockMvc
- **Deployment**: Render (render.yaml)

## Database Migration Note

⚠️ **Important**: The project was migrated from SQLite to PostgreSQL (commit `34c52d0`).

- **AGENTS.md** still references SQLite configuration (outdated)
- **Actual implementation** uses PostgreSQL
- **Schema**: `schema.sql` works for both SQLite and PostgreSQL
- **Connection**: Uses `${DATABASE_URL}` from environment

## Development Environment Setup

### Prerequisites

```bash
# Java 21
java --version  # Should show Java 21

# Maven (included in pom.xml via Spring Boot)
./mvnw --version

# PostgreSQL (for local development)
# Set DATABASE_URL environment variable
export DATABASE_URL="jdbc:postgresql://localhost:5432/urlshortener"
```

### Build and Run

```bash
# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### Database Setup

**Local PostgreSQL**:
```bash
# Create database
createdb urlshortener

# Set environment variable
export DATABASE_URL="jdbc:postgresql://localhost:5432/urlshortener"

# Schema is auto-applied via Spring (spring.sql.init.mode=always)
```

**Render (Production)**:
- DATABASE_URL is auto-injected by Render PostgreSQL addon
- Schema is applied on startup

## Paperclip Integration

### Agent Configuration

- **Agent ID**: `f4bafd02-62fe-4ad4-bfe2-0d00a4885ccd`
- **Company ID**: `3aa10448-f23d-43ce-98bc-559ea1cb27d8`
- **Role**: `engineer` (Founding Engineer)
- **Reports To**: CEO (`cf199286-1abc-4847-a152-b66535521cca`)
- **Working Directory**: `/Users/sahiljain/projects/url-shortener`
- **Instructions**: `AGENTS.md`

### Task Management Workflow

1. **Check Inbox**: Use `paperclip` skill with `inbox` command
2. **Checkout Task**: Paperclip automatically checks out assigned tasks
3. **Do Work**: Implement features, write tests, commit code
4. **Update Status**: Mark tasks `done` with summary comment, or `blocked` with explanation
5. **Delegate**: Create subtasks when needed

### Authentication

```bash
# Get Paperclip credentials for local CLI mode
cd /Users/sahiljain/.claude/skills/paperclip
pnpm paperclipai agent local-cli f4bafd02-62fe-4ad4-bfe2-0d00a4885ccd \
  --company-id 3aa10448-f23d-43ce-98bc-559ea1cb27d8

# Exports PAPERCLIP_API_KEY and other env vars
```

### Heartbeat Execution

The agent runs in **heartbeat mode**:
- **Interval**: Every 30 minutes (1800 seconds)
- **Cooldown**: 10 seconds between runs
- **Wake on Demand**: Yes (task assignments, mentions trigger immediate wake)
- **Max Concurrent Runs**: 1

### API Access

All Paperclip API calls use:
- **Base URL**: `http://127.0.0.1:3100`
- **Auth**: `Authorization: Bearer $PAPERCLIP_API_KEY`
- **Run Tracking**: `X-Paperclip-Run-Id: $PAPERCLIP_RUN_ID` (for mutations)

## Git Workflow

### Commit Convention

```
type: short description

[optional body]

Co-Authored-By: Paperclip <noreply@paperclip.ing>
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

### Recent Commits

```
34c52d0 feat: migrate to PostgreSQL, add Render deploy config
c9f8c91 feat: implement URL shortener with Java Spring Boot
```

### Branch Strategy

- **Main branch**: `main` (production)
- **Working branch**: Create feature branches as needed
- **PR target**: `main`

## Testing

### Run Tests

```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=UrlControllerTest
```

### Test Database

Tests use H2 in-memory database (no PostgreSQL needed for tests).

## Deployment

### Render Configuration

- **Config File**: `render.yaml`
- **Database**: PostgreSQL addon (auto-provisioned)
- **Build Command**: `./mvnw clean package`
- **Start Command**: `java -jar target/url-shortener-0.0.1-SNAPSHOT.jar`
- **Port**: Dynamic (from `$PORT` env var)

### Deploy

```bash
# Push to main triggers auto-deploy on Render
git push origin main
```

## Success Criteria (VIB-2)

✅ **Can run Paperclip locally**: Confirmed via successful heartbeat execution
✅ **Can execute tasks via heartbeat**: VIB-2 checked out and in progress
✅ **Workspace documentation created**: This file (`WORKSPACE.md`)

## Next Steps

Once VIB-2 is complete, the engineer can proceed with:
- VIB-3: Build vibecoding proof of concept
- VIB-4: Document technical architecture and patterns

## Troubleshooting

### Paperclip Authentication Issues

```bash
# Re-authenticate
cd /Users/sahiljain/.claude/skills/paperclip
pnpm paperclipai agent local-cli $PAPERCLIP_AGENT_ID --company-id $PAPERCLIP_COMPANY_ID
```

### Database Connection Issues

```bash
# Check DATABASE_URL is set
echo $DATABASE_URL

# Verify PostgreSQL is running
psql -c "SELECT version();"
```

### Build Issues

```bash
# Clean and rebuild
./mvnw clean package

# Skip tests if needed
./mvnw clean package -DskipTests
```

## Resources

- **Agent Instructions**: `AGENTS.md` (needs update for PostgreSQL)
- **Paperclip Skill**: `/Users/sahiljain/.claude/skills/paperclip`
- **Paperclip Codebase**: `/Users/sahiljain/paperclip`
- **Company Dashboard**: Via Paperclip API `/api/companies/{companyId}/dashboard`
