# qvideo - app for manage video streaming

# GitHub Copilot Instructions
This document directs GitHub Copilot to generate code consistent with our architecture, conventions, and best practices.

## Project Overview
- **Stack**: Java 21, Quarkus 3.x, MongoDB (Panache Reactive), Mutiny & Vert.x
- **Purpose**: Stream video content via RESTful microservices supporting channel management, live streaming, PVR recordings, and DRM key distribution.

## Module Structure
quantum.video

- **api**: DTOs, paging models, API contracts (`*.java` in `quantum/music/api`)
- **model**: Domain entities and shapes (`quantum/music/model`)
- **repository**: Panache Reactive repositories (`PanacheRepositoryBase<Entity, ID>`) using MongoDB (`quantum/music/repository`)
- **resource**: JAX-RS endpoints (`@Path`, `@Produces(MediaType.APPLICATION_JSON)`) — thin controllers delegating to services (`quantum/music/resource`)
  Use paging models for list responses, e.g., `PageResponse<PlaybackChannel>`. Always use api DTOs for request/response bodies. Never use clases from model package in resources.
- **service**: Business logic with CDI (`@ApplicationScoped`, `@Inject`) and Mutiny (`Uni`, `Multi`) (`quantum/music/service`)
  Use `@Transactional` for methods that modify data. Use PagedData for paginated queries.

## Coding Standards
- **Formatting**: 4-space indentation, UTF-8, one statement per line
- **Naming**: PascalCase for classes, camelCase for methods/fields
- **Imports**: Explicit only (no wildcards)
- **DI**: Use CDI annotations (`@Inject`, `@ApplicationScoped`)
- **Validation**: Bean Validation (`@Valid`, `@NotNull`, etc.) on DTOs
- **Error Handling**: Return meaningful HTTP status codes & error DTOs

## Reactive Programming
- Use Mutiny types (`Uni<T>`, `Multi<T>`) for async flows
- Prefer non-blocking I/O in repositories and services
- Handle back-pressure and error signals explicitly
- Example pipeline:
  ```java
  public Uni<List<Channel>> channels() {
    return repository.streamAll()
                      .onItem().transform(Channel::toDto)
                      .collect().asList();
  }
  ```

## REST API Conventions
- **Media Type**: JSON (`MediaType.APPLICATION_JSON`)
- **HTTP Verbs**: `@GET`, `@POST`, `@PUT`, `@DELETE`
- **Paths**: `/channels`, `/live`, `/pvr`, `/drm`
- **Responses**: Return `Uni<Response>` or DTO directly
- Keep resources minimal; delegate to service layer

## Testing Guidelines
- **Unit Tests**: JUnit 5 in `src/test/java/quantum/video/service` or `utils`
- **Mocking**: Mockito for dependencies
- **Integration Tests**: RestAssured or Quarkus test framework in `src/test/java`
- **Naming**: `{ClassName}Test`, descriptive method names

## Configuration & Build
- **Config File**: `src/main/resources/application.properties`
- **Secrets**: `secret.jwk` (exclude from git)
- **Local Run**: `./mvnw clean package quarkus:dev`
- **Docker**: Dockerfiles under `src/main/docker` (JVM, native, multistage, legacy)

## Copilot Suggestions
- Follow existing patterns for resources, services, repositories
- Suggest imports, annotations, and method signatures aligned with project style
- Prioritize readability, maintainability, minimal dependencies
- Avoid wildcard imports and redundant code
- Reference existing classes/tests for guidance

Commit Messages
Use Conventional Commits format:

feat: for new features
fix: for bug fixes
chore: for maintenance tasks (build, configs, dependencies)
refactor: for code restructuring
docs: for documentation
test: for tests
Keep messages concise and in English when appropriate.

Happy coding with GitHub Copilot!
