# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository structure

Monorepo with two independently versioned apps plus Terraform infra:

- `backend/` — Spring Boot 4.1 (Java 25) REST API. See `backend/コーディング規約.md` (index) and `backend/docs/*.md` for the full, detailed coding standard — read the relevant doc before making non-trivial changes in that area (entity design, exceptions, security, logging, filters, etc.) rather than relying on memory of this file.
- `frontend/` — Next.js 16 (App Router) + TypeScript. Calls the backend over HTTP with cookie-based sessions.
- `infrastructure/` — Terraform (AWS: VPC / ECR / RDS / ALB / ECS / CodePipeline).

## Commands

### Backend (run from `backend/`)

```bash
docker-compose up -d                                   # start Postgres + Redis
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=changeme ./gradlew bootRun

./gradlew test                                          # all tests
./gradlew test --tests "ProductServiceTest"              # single test class
./gradlew test --tests "ProductServiceTest.getProduct_whenNotFound_shouldThrow"  # single test method

./gradlew spotlessCheck                                  # format check (CI runs this)
./gradlew spotlessApply                                  # auto-format
./gradlew test jacocoTestReport                           # coverage report -> build/reports/jacoco/test/html/index.html
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Local profile seeds sample data (login `admin@example.com` / `password`).

### Frontend (run from `frontend/`)

```bash
npm install
npm run dev                 # dev server (localhost:3000)
npm run build
npm run lint                 # ESLint
npm run format                # Prettier write
npm run format:check          # Prettier check (CI)
npm run generate:api-types     # regenerate src/lib/api/schema.d.ts from backend's OpenAPI spec (backend must be running)
```

There is no frontend test suite currently. Regenerate API types after any backend API change — this is a common source of type-mismatch build errors.

### CI

`.github/workflows/ci.yml` runs `spotlessCheck` then `test` for `backend/` only, against real Postgres/Redis service containers, on push/PR to `main`.

## Backend architecture

Layered architecture: `Controller -> Service -> Repository`, two parallel API surfaces (`admin` and `customer`) under `com.example.ecapi`.

- `controller/{admin,customer}/dto` + `mapper` — HTTP-facing request/response DTOs (validation annotations live only here) and `{Resource}ApiMapper` converting to/from service DTOs.
- `service/**/dto` + `mapper` — business input/output DTOs (`{Action}{Resource}` in, `{Resource}Result` out), no HTTP or validation concerns, `{Resource}EntityMapper` converts to/from entities.
- `entity/` — JPA entities, no Spring/HTTP dependency.
- `exception/` — all business exceptions extend `BusinessException` (abstract, carries `HttpStatus`) via intermediate classes `ResourceNotFoundException` (404) / `ConflictException` (409); `GlobalExceptionHandler` handles the whole hierarchy with one `@ExceptionHandler(BusinessException.class)` — never add per-exception handlers.
- `config/` — `@Bean`/`@Configuration`/`@Aspect` only, no business logic.

Key non-obvious conventions (full detail in `backend/docs/`):

- **No `ServiceImpl` pattern**; interfaces only when multiple implementations genuinely exist. Constructor injection only, never `@Autowired` field injection.
- **Entities**: extend `BaseEntity` (version/createdAt/updatedAt/createdBy/updatedBy via `@MappedSuperclass` + JPA Auditing). No `@Builder`/`@Data` on entities. `createdBy`/`updatedBy` are set only by `AuditorAware` — never set manually from Service/Controller. `SecurityContextHolder` is read only in `AuditorAware` and in Filters, never in Service/Repository.
- **Soft delete**: `deleted` boolean field (DB column `is_deleted`); repository queries must filter `deleted = false` explicitly (e.g. `findByIdAndDeletedFalse`).
- **Optimistic locking**: `version` on every entity via `BaseEntity`. UPDATE flows accept `version` from the client and set it on the entity before mutating (lets Hibernate's implicit `WHERE version = ?` produce a conflict); DELETE flows deliberately skip version handling. Client-forced overwrites (admin override, batch) simply don't call `setVersion()`.
- **N+1 defense**: `default_batch_fetch_size: 100` is set globally as a safety net, but new paginated queries should still use `@EntityGraph` and complex/DTO-projecting queries `JOIN FETCH`. Bulk `@Modifying` queries require `clearAutomatically = true` and must increment `version` manually in the JPQL.
- **Bidirectional `@OneToMany`**: owning side (`@ManyToOne`) holds the FK; parent exposes only `addXxx`/`removeXxx` helpers and returns an unmodifiable collection from the getter — Service code never mutates the collection directly.
- **Filters vs Service vs SecurityConfig**: cross-cutting, DB-free, pre-auth concerns (request tracing/MDC, access logging, rate limiting, maintenance mode) go in `OncePerRequestFilter`s registered via `SecurityConfig.addFilterBefore/After` — never `@Component` (would double-register). Anything needing business/DB data belongs in Service. Authn/authz belongs in `SecurityConfig`; 401/403 responses are wired via `exceptionHandling()`, not `@ControllerAdvice`.
- **Logging**: access logs from `RequestLoggingFilter`, per-request-method timing from `ServiceLoggingAspect` (DEBUG, automatic), business events logged manually at INFO with resource IDs. Never log full request/response bodies, entities via `toString()`, or fields in `backend/docs/12-logging.md`'s masking table (password, token, email, phone, address, card data, Authorization/Cookie headers). `GlobalExceptionHandler` is the only place that logs exceptions — don't log-and-rethrow in Service.
- **REST API design** (`backend/API_INTERFACE_CONVENTIONS.md`): state transitions with business rules are actions (`POST /orders/{id}/cancel`), not raw `PUT` status writes; GET is side-effect-free and never carries PII in query params.
- Package/class/method naming, DTO/mapper conventions, DDL conventions, virtual-thread pinning rules (no `synchronized` around I/O — use `ReentrantLock`), and the PR checklist are in `backend/docs/01` through `13` and `backend/docs/appendix-checklist.md` — consult the specific doc rather than guessing.

## Frontend architecture

- `src/app/` — App Router pages; directory structure is the URL structure. Customer-facing routes at the root (`/products`, `/cart`, `/orders`, `/mypage`, ...), admin routes under `/admin/**` sharing the layout in `src/app/admin/layout.tsx` (nav bar; `/admin/login` is the one exception rendered without it).
- `src/lib/api/` — all backend calls go through `apiFetch` in `client.ts` (adds `credentials: "include"`, JSON headers, throws `ApiError` with status/code/details on non-2xx, redirects to the appropriate login page on 401 unless `suppressAuthRedirect` is set). Per-resource files (`products.ts`, `cart.ts`, `orders.ts`, `admin*.ts`, ...) wrap `apiFetch`. `schema.d.ts` is generated from the backend's OpenAPI spec — never hand-edit it, run `npm run generate:api-types` instead.
- Two independent auth flows/cookies: admin (`/api/auth/*`) and customer (`/api/customer/auth/*`) — `auth.ts` vs `customerAuth.ts`, and `apiFetch`'s 401 redirect picks `/admin/login` vs `/login` based on whether the path starts with `/api/admin` or `/api/auth`.
- Path alias `@/*` maps to `src/*`.