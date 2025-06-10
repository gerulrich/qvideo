# GitHub Copilot Instructions for qvideo

This document guides Copilot to generate code consistent with the qvideo architecture, conventions, and best practices.

---

## Project Overview

- **Stack**: Java 21, Quarkus 3.x, MongoDB (Panache Reactive), Mutiny & Vert.x
- **Purpose**: Microservices for video streaming: channel management, live streaming, PVR recordings, DRM key distribution

## Repository Layout

```
src/
  main/
    java/quantum/music/
      api/       → DTOs, API contracts
      model/     → Domain entities
      repository/→ Panache reactive repositories
      resource/  → JAX-RS endpoints (controllers)
      service/   → Business logic (Mutiny, CDI)
    resources/   → application.properties, banner, JWK secret

src/
  test/
    java/quantum/music/ → Unit & integration tests
```

## Coding Standards

- **Formatting**: 4-space indentation, UTF-8, one statement per line
- **Naming**: `PascalCase` for classes, `camelCase` for methods/fields
- **Imports**: Explicit imports (no wildcards)
- **Dependency Injection**: CDI annotations (`@Inject`, `@ApplicationScoped`)
- **Validation**: Bean Validation (`@NotNull`, `@Valid`) on DTOs
- **Transactions**: Mark methods that modify data with `@Transactional`
- **Error Handling**: Throw meaningful exceptions (`NotFoundException`, custom errors) and map to HTTP status codes

## Module Responsibilities

- **api**: Define request/response DTOs and paging contracts. Do not use model classes here.
- **model**: Panache entities or POJOs mirroring MongoDB collections.
- **repository**: Extend `PanacheRepositoryBase<Entity, ID>` for reactive Mongo access.
- **service**: Business logic using `Uni<T>`/`Multi<T>`, assemble domain operations, apply validations.
- **resource**: Expose REST endpoints (`@Path`, `@Produces(MediaType.APPLICATION_JSON)`), delegate to service layer, return DTOs or `Response` wrapped in `Uni`

## Reactive & Mutiny Patterns

- Return `Uni<T>` or `Multi<T>` for async flows.
- Chain operations with `.onItem().transform(...)`, `.onItem().produceUni(...)`, `.onFailure().recoverWith...`.
- Collect results with `.collect().asList()` or return streaming for large datasets.

## REST API Conventions

- **Media Type**: JSON (`@Produces(MediaType.APPLICATION_JSON)`, `@Consumes` where needed)
- **HTTP Methods**: `@GET`, `@POST`, `@PUT`, `@DELETE`
- **Paths**: Use plural nouns (e.g., `/channels`, `/programs`, `/live`)
- **Pagination**: Accept page parameters, return a paging model (`PageResponse<T>`) in `api` package
- **Errors**: Return `Response.status(...)` with error DTO

## Testing Guidelines

- **Unit Tests**: JUnit 5 & Mockito in `src/test/java/quantum/music/service` and `utils`
- **Integration Tests**: Quarkus test framework or RestAssured in `src/test/java/quantum/music/resource`
- **Naming**: `{ClassName}Test`, methods describe scenario and expected result
- **Coverage**: Focus on business logic and REST endpoints

## Configuration & Build

- **Properties**: `src/main/resources/application.properties` for Mongo URI, Quarkus settings
- **Secrets**: `secret.jwk` for DRM (excluded from VCS)
- **Local Run**: `./mvnw clean package quarkus:dev`
- **Docker**: Maintained under `src/main/docker` (JVM, native, multistage)

## Copilot Suggestions

- Follow patterns in existing service and resource classes.
- Suggest imports, annotations, and method signatures aligned with this guide.
- Avoid wildcard imports and redundant code.
- Use existing DTOs/tests as reference.
- Keep code readable, maintainable, minimal dependencies.

---

Happy coding with GitHub Copilot!

