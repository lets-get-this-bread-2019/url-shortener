# URL Shortener - Technical Architecture

**Project**: URL Shortener Service
**Stack**: Java 21 + Spring Boot 3.2.3 + PostgreSQL
**Last Updated**: 2026-03-22

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Code Organization](#code-organization)
3. [Design Patterns](#design-patterns)
4. [Development Workflow](#development-workflow)
5. [Testing Strategy](#testing-strategy)
6. [Deployment](#deployment)
7. [Database](#database)
8. [API Design](#api-design)
9. [Security Considerations](#security-considerations)
10. [Future Enhancements](#future-enhancements)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────┐
│   Client    │
│ (HTTP/cURL) │
└──────┬──────┘
       │
       │ HTTP/HTTPS
       ▼
┌─────────────────────────────────┐
│      Spring Boot App            │
│                                 │
│  ┌─────────────────────────┐   │
│  │  UrlController          │   │  ← REST endpoints
│  │  - POST /shorten        │   │
│  │  - GET /{code}          │   │
│  └───────────┬─────────────┘   │
│              │                  │
│  ┌───────────▼─────────────┐   │
│  │  UrlService             │   │  ← Business logic
│  │  - createShortUrl()     │   │
│  │  - findByCode()         │   │
│  └───────────┬─────────────┘   │
│              │                  │
│  ┌───────────▼─────────────┐   │
│  │  UrlRepository          │   │  ← Data access
│  │  (Spring Data JDBC)     │   │
│  └───────────┬─────────────┘   │
└──────────────┼─────────────────┘
               │
               │ JDBC
               ▼
┌─────────────────────────────────┐
│      PostgreSQL Database        │
│                                 │
│  Table: short_urls              │
│  - id (SERIAL PRIMARY KEY)      │
│  - code (TEXT UNIQUE)           │
│  - original_url (TEXT)          │
│  - created_at (TIMESTAMP)       │
└─────────────────────────────────┘
```

### Technology Stack

| Layer              | Technology                   | Version     | Purpose                        |
|--------------------|------------------------------|-------------|--------------------------------|
| **Language**       | Java                         | 21          | Modern Java features (records) |
| **Framework**      | Spring Boot                  | 3.2.3       | Web framework, DI, REST        |
| **Data Access**    | Spring Data JDBC             | (included)  | Repository pattern             |
| **Database**       | PostgreSQL                   | 16+         | Production data store          |
| **Test Database**  | H2                           | (latest)    | In-memory for tests            |
| **Build Tool**     | Maven                        | 3.9+        | Dependency management          |
| **Testing**        | JUnit 5 + Spring MockMvc     | 5.x         | Unit + integration tests       |
| **Deployment**     | Render (PaaS)                | -           | Cloud hosting                  |
| **Containerization** | Docker                     | -           | Optional local dev             |

### Deployment Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Render Platform                      │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Web Service (auto-scaled)                         │ │
│  │                                                    │ │
│  │  ┌──────────────────────────────────────────────┐ │ │
│  │  │  URL Shortener (Java Spring Boot)            │ │ │
│  │  │  - Port: $PORT (dynamic, typically 10000)    │ │ │
│  │  │  - Health check: GET /                       │ │ │
│  │  └────────────────┬─────────────────────────────┘ │ │
│  │                   │                               │ │
│  │                   │ $DATABASE_URL                 │ │
│  │                   ▼                               │ │
│  │  ┌──────────────────────────────────────────────┐ │ │
│  │  │  PostgreSQL Database (managed)               │ │ │
│  │  │  - Auto-backups                              │ │ │
│  │  │  - Connection pooling                        │ │ │
│  │  └──────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│  Public URL: https://<app-name>.onrender.com            │
└──────────────────────────────────────────────────────────┘
```

---

## Code Organization

### Directory Structure

```
url-shortener/
├── src/
│   ├── main/
│   │   ├── java/com/example/urlshortener/
│   │   │   ├── UrlShortenerApplication.java    # Main entry point
│   │   │   ├── controller/
│   │   │   │   ├── UrlController.java          # REST API endpoints
│   │   │   │   └── HomeController.java         # Root/health endpoint
│   │   │   ├── service/
│   │   │   │   └── UrlService.java             # Business logic
│   │   │   ├── repository/
│   │   │   │   └── UrlRepository.java          # Data access interface
│   │   │   └── model/
│   │   │       └── ShortUrl.java               # Domain model (record)
│   │   └── resources/
│   │       ├── application.properties          # Spring config
│   │       ├── schema.sql                      # Database schema
│   │       └── static/
│   │           └── index.html                  # Welcome page
│   └── test/
│       └── java/com/example/urlshortener/
│           └── UrlControllerTest.java          # Integration tests
├── pom.xml                                      # Maven dependencies
├── Dockerfile                                   # Container definition
├── render.yaml                                  # Render deployment config
├── AGENTS.md                                    # AI agent instructions
├── WORKSPACE.md                                 # Workspace setup guide
├── VIBECODING_POC.md                            # Vibecoding demonstration
└── ARCHITECTURE.md                              # This file
```

### Package Structure (by Layer)

**Controller Layer** (`controller/`)
- Handles HTTP requests/responses
- Input validation (basic)
- Error handling (400, 404, 409)
- Maps DTOs to service calls

**Service Layer** (`service/`)
- Business logic
- Advanced validation (custom codes)
- Code generation (Base62)
- Conflict detection
- Exception throwing

**Repository Layer** (`repository/`)
- Data access abstraction
- Spring Data JDBC interface
- Query methods (`findByCode`)

**Model Layer** (`model/`)
- Domain objects
- Database entity mapping
- Immutable records (Java 21)

---

## Design Patterns

### 1. Layered Architecture

**Pattern**: 3-tier layered architecture (Controller → Service → Repository)

**Benefits**:
- Clear separation of concerns
- Easy to test (mock layers)
- Maintainable (change one layer without affecting others)
- Standard Spring Boot convention

**Implementation**:
```
UrlController → UrlService → UrlRepository → Database
   (HTTP)      (Business)     (Data Access)
```

### 2. Repository Pattern

**Pattern**: Spring Data JDBC repository

**Benefits**:
- Abstraction over database operations
- Type-safe queries
- Automatic CRUD operations
- Easy to mock for testing

**Implementation**:
```java
public interface UrlRepository extends CrudRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByCode(String code);
}
```

### 3. Record Pattern (Java 21)

**Pattern**: Immutable data carriers using Java records

**Benefits**:
- Concise syntax (no boilerplate)
- Immutable by default (thread-safe)
- Value semantics (equals, hashCode, toString)
- Perfect for DTOs and entities

**Implementation**:
```java
@Table("short_urls")
public record ShortUrl(
    @Id Long id,
    String code,
    String originalUrl,
    String createdAt
) {
    public ShortUrl(String code, String originalUrl) {
        this(null, code, originalUrl, null);
    }
}
```

### 4. Exception-Driven Flow Control

**Pattern**: Custom exceptions for business rule violations

**Benefits**:
- Clear error semantics
- Centralized error handling
- HTTP status mapping (400, 409)

**Implementation**:
```java
// Service throws
throw new InvalidCodeException("Code too short");
throw new CodeAlreadyExistsException("duplicate");

// Controller catches and maps to HTTP
catch (InvalidCodeException e) → 400 Bad Request
catch (CodeAlreadyExistsException e) → 409 Conflict
```

### 5. Base62 Encoding

**Pattern**: ID encoding for URL-safe short codes

**Benefits**:
- URL-safe (alphanumeric only)
- Short codes (7 chars for millions of URLs)
- No collisions (derived from unique DB ID)
- Familiar to users (like YouTube IDs)

**Implementation**:
```java
private static final String BASE62 = "0-9A-Za-z";
String code = encodeBase62(dbId).leftPad(7);
```

### 6. Dependency Injection

**Pattern**: Constructor-based dependency injection

**Benefits**:
- Testable (easy to mock)
- Immutable dependencies
- Clear dependencies at construction time

**Implementation**:
```java
@RestController
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }
}
```

---

## Development Workflow

### Local Development Setup

```bash
# 1. Clone repository
git clone <repo-url>
cd url-shortener

# 2. Set up PostgreSQL (optional, can use H2)
createdb urlshortener
export DATABASE_URL="jdbc:postgresql://localhost:5432/urlshortener"

# 3. Build
mvn clean package

# 4. Run locally
mvn spring-boot:run

# 5. Test
mvn test

# 6. Access
curl http://localhost:8080
```

### Git Workflow

**Branch Strategy**: Main-only (simple projects) or feature branches

**Commit Convention**:
```
type: short description

[optional body]

Co-Authored-By: Paperclip <noreply@paperclip.ing>
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Examples**:
```
feat: add custom short code feature
fix: handle null URL gracefully
docs: add architecture documentation
refactor: extract validation logic
test: add conflict scenario tests
chore: update Spring Boot to 3.2.4
```

### Code Review Checklist

- [ ] Tests pass (`mvn test`)
- [ ] No security vulnerabilities (SQL injection, XSS)
- [ ] Error handling covers edge cases
- [ ] Backward compatibility maintained
- [ ] Documentation updated
- [ ] Commit message follows convention

---

## Testing Strategy

### Test Pyramid

```
         ┌───────────────────┐
         │  Manual Testing   │  ← Minimal
         └───────────────────┘
        ┌─────────────────────┐
        │  Integration Tests  │  ← Primary focus
        └─────────────────────┘
       ┌───────────────────────┐
       │   Unit Tests          │  ← Some coverage
       └───────────────────────┘
```

### Test Levels

**1. Integration Tests** (primary)
- Uses `@SpringBootTest` + `MockMvc`
- Tests full HTTP request/response cycle
- Uses H2 in-memory database
- Covers controller + service + repository layers
- Fast enough to run on every commit

**2. Unit Tests** (minimal)
- Currently not separated from integration tests
- Future: Test service layer in isolation

**3. Manual Tests** (smoke testing)
- Deploy to staging/production
- Test critical paths manually
- Verify redirects work end-to-end

### Test Coverage

**Current Tests** (11 total):

| Test | Coverage |
|------|----------|
| `testShortenUrl` | Happy path: auto-generated code |
| `testShortenUrlWithInvalidInput` | Validation: empty URL |
| `testRedirectToOriginalUrl` | Redirect: auto-generated code |
| `testRedirectWithNonExistentCode` | Error: 404 not found |
| `testShortenUrlWithCustomCode` | Happy path: custom code |
| `testCustomCodeConflict` | Error: 409 conflict |
| `testCustomCodeTooShort` | Validation: code too short |
| `testCustomCodeTooLong` | Validation: code too long |
| `testCustomCodeWithInvalidCharacters` | Validation: invalid chars |
| `testCustomCodeReservedWord` | Validation: reserved word |
| `testRedirectWithCustomCode` | Redirect: custom code |

**Coverage Areas**:
- ✅ Happy paths (auto + custom codes)
- ✅ Validation errors (all rules)
- ✅ Conflict handling (409)
- ✅ Not found (404)
- ✅ Redirects (302)

**Not Covered** (future):
- ❌ Performance testing (load tests)
- ❌ Security testing (injection attacks)
- ❌ Concurrency testing (race conditions)

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=UrlControllerTest

# Specific test method
mvn test -Dtest=UrlControllerTest#testShortenUrl

# With coverage report (future)
mvn test jacoco:report
```

---

## Deployment

### Render Configuration

**File**: `render.yaml`

```yaml
services:
  - type: web
    name: url-shortener
    runtime: java
    buildCommand: ./mvnw clean package
    startCommand: java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: url-shortener-db
          property: connectionString

databases:
  - name: url-shortener-db
    databaseName: urlshortener
    user: urlshortener
```

### Environment Variables

| Variable | Source | Purpose |
|----------|--------|---------|
| `DATABASE_URL` | Render PostgreSQL addon | JDBC connection string |
| `PORT` | Render (auto-injected) | HTTP port (e.g., 10000) |

### Build Process

1. **Trigger**: Push to `main` branch (or manual deploy)
2. **Build**: `./mvnw clean package` (creates JAR)
3. **Start**: `java -jar target/url-shortener-0.0.1-SNAPSHOT.jar`
4. **Health Check**: Render pings `GET /` until 200 OK
5. **Live**: Traffic routed to new instance

### Deployment Checklist

- [ ] Tests pass locally (`mvn test`)
- [ ] Database migrations applied (`schema.sql`)
- [ ] Environment variables configured
- [ ] Health check endpoint works
- [ ] Render build succeeds
- [ ] Smoke test: create + redirect URL
- [ ] Monitor logs for errors

---

## Database

### Schema

**File**: `src/main/resources/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS short_urls (
  id SERIAL PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  original_url TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_short_urls_code ON short_urls(code);
```

### Schema Design Decisions

**1. `id` (SERIAL)**
- Auto-increment primary key
- Used for Base62 encoding (future)
- Not exposed in API

**2. `code` (TEXT UNIQUE)**
- The short code (e.g., "abc1234", "mylink")
- Unique constraint prevents duplicates
- Indexed for fast lookups
- Case-sensitive

**3. `original_url` (TEXT)**
- The full destination URL
- No length limit (TEXT supports long URLs)
- Not validated for HTTPS (accepts any scheme)

**4. `created_at` (TIMESTAMP)**
- Automatic timestamp on insert
- Useful for analytics, debugging
- Default: server time

### Indexes

- **Primary Key**: `id` (automatic index)
- **Unique Constraint**: `code` (automatic index)
- **Explicit Index**: `idx_short_urls_code` (query optimization)

**Query Pattern**:
```sql
SELECT * FROM short_urls WHERE code = 'abc1234';
```
→ Uses `idx_short_urls_code` index (O(log n) lookup)

### Migration Strategy

**Current**: `spring.sql.init.mode=always`
- Schema created on every startup
- `CREATE TABLE IF NOT EXISTS` prevents errors
- Simple for initial development

**Future** (recommended for production):
- Use Flyway or Liquibase for versioned migrations
- Track schema changes in version control
- Rollback support

---

## API Design

### Endpoints

#### 1. POST /shorten

**Purpose**: Create a short URL

**Request**:
```json
{
  "url": "https://example.com",
  "customCode": "mylink"  // optional
}
```

**Response** (200 OK):
```json
{
  "shortUrl": "http://localhost:8080/mylink",
  "code": "mylink"
}
```

**Errors**:
- `400 Bad Request`: Invalid URL, invalid custom code
- `409 Conflict`: Custom code already exists

**Validation**:
- URL: Required, non-blank
- Custom code: 3-20 chars, alphanumeric, not reserved

#### 2. GET /{code}

**Purpose**: Redirect to original URL

**Request**: `GET /abc1234`

**Response** (302 Found):
```
Location: https://example.com
```

**Errors**:
- `404 Not Found`: Code doesn't exist

**Performance**: Indexed lookup (O(log n))

### API Design Principles

**1. RESTful**
- `POST` for creation
- `GET` for retrieval
- Proper HTTP status codes

**2. Backward Compatible**
- Adding `customCode` doesn't break existing clients
- Response includes both `shortUrl` and `code`

**3. Error-Friendly**
- Clear error messages in response body
- Semantic HTTP status codes (400, 404, 409)

**4. Stateless**
- No sessions required
- Each request is independent

**5. Idempotent (partially)**
- `GET /{code}` is idempotent
- `POST /shorten` with same custom code → 409 (not idempotent)

---

## Security Considerations

### Current Security Measures

**1. Input Validation**
- URL: Required, non-blank
- Custom code: Alphanumeric only (prevents injection)

**2. Reserved Codes**
- Blocks `api`, `admin`, etc. to prevent path conflicts

**3. No Code Disclosure**
- Auto-generated codes are random (not sequential)
- Hard to guess other users' URLs

### Security Features Implemented

**1. ✅ Open Redirect Protection**
- **Implementation**: URL scheme validation in `UrlService.validateUrl()`
- **Allows**: Only `http://` and `https://` schemes
- **Commit**: `ca5e744` (2026-03-22)

**2. ✅ Rate Limiting**
- **Implementation**: IP-based rate limiting using Bucket4j (`RateLimitService`)
- **Limits**: 10 requests/min for URL creation, 100 requests/min for redirects
- **Enforced**: Via `RateLimitInterceptor` on both endpoints
- **Commit**: `efb530b` (2026-03-22)

**3. ✅ URL Expiration with TTL**
- **Implementation**: Optional `expires_at` column, scheduled cleanup job
- **Features**: Custom TTL per URL, automatic expired URL deletion
- **Service**: `UrlCleanupService` runs cleanup every 5 minutes
- **Commit**: `af59a15` (2026-03-22)

**4. ✅ SQL Injection Prevention**
- **Implementation**: Spring Data JDBC uses parameterized queries
- **Status**: Safe by default, no raw SQL used

### Remaining Security Considerations

**1. HTTPS Enforcement**
- **Current**: Accepts both `http://` and `https://` in shortened URLs
- **Mitigation**: Render platform provides HTTPS by default for the service
- **Future**: Could enforce HTTPS-only for submitted URLs

---

## Future Enhancements

### Completed ✅

1. **URL Expiration** (commit `af59a15`)
   - ✅ Added `expires_at` column
   - ✅ Scheduled job to delete expired URLs (`UrlCleanupService`)
   - ✅ API accepts optional `ttl` parameter

2. **Rate Limiting** (commit `efb530b`)
   - ✅ IP-based rate limiting using Bucket4j
   - ✅ Configurable limits via `application.properties`
   - ✅ Separate limits for shorten (10/min) and redirect (100/min)

3. **Open Redirect Protection** (commit `ca5e744`)
   - ✅ URL scheme validation (http/https only)
   - ✅ Prevents malicious redirects

### High Priority

1. **Click Analytics**
   - Track click count, last accessed, referrer
   - Add `clicks` table (url_id, timestamp, ip, user_agent)
   - Provide analytics dashboard endpoint

### Medium Priority

4. **Custom Domains**
   - Support `short.ly/abc123` style URLs
   - Multi-tenant architecture

5. **URL Preview**
   - `GET /{code}/preview` shows destination before redirect
   - Protects against phishing

6. **Bulk URL Shortening**
   - `POST /shorten/bulk` with array of URLs
   - CSV upload support

### Low Priority

7. **Admin Dashboard**
   - Web UI to manage URLs
   - Search, filter, delete URLs

8. **API Authentication**
   - Require API key for URL creation
   - User accounts

9. **QR Code Generation**
   - `GET /{code}.qr` returns QR code image
   - Useful for print materials

---

## Appendix: Key Files

### application.properties

```properties
spring.datasource.url=${DATABASE_URL}
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
server.port=${PORT:8080}
spring.jpa.hibernate.ddl-auto=none
```

### pom.xml (Key Dependencies)

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
  </dependency>
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## Conclusion

This URL shortener demonstrates:
- ✅ Clean 3-tier architecture (Controller → Service → Repository)
- ✅ Modern Java 21 features (records, text blocks)
- ✅ Spring Boot best practices
- ✅ Comprehensive testing (11 tests, all passing)
- ✅ Production deployment (Render + PostgreSQL)
- ✅ Autonomous development (vibecoding)

The architecture is **simple, maintainable, and scalable** — ready for future enhancements without major refactoring.
