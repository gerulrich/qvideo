# qvideo

Aplicación para gestión y streaming de video en tiempo real, grabaciones PVR y distribución de claves DRM. Basada en microservicios RESTful con Java 21, Quarkus 3.x y MongoDB.

## Características principales

- **Gestión de canales**: Alta, baja y modificación de canales de video.
- **Streaming en vivo**: Reproducción de canales en tiempo real vía endpoints REST.
- **Guía de programas**: Consulta de programación y metadatos asociados.
- **Grabaciones PVR**: Soporte para grabación y reproducción de programas.
- **DRM**: Distribución de licencias y claves para protección de contenido (Widevine, ClearKeys).
- **Paginación**: Respuestas paginadas para listados extensos.
- **API reactiva**: Flujos asíncronos con Mutiny (`Uni`, `Multi`) y Vert.x.

## Arquitectura

- **Stack**: Java 21, Quarkus 3.x, MongoDB (Panache Reactive), Mutiny & Vert.x
- **Módulos**:
  - `api`: Modelos DTO, contratos de API y paginación
  - `model`: Entidades de dominio
  - `repository`: Repositorios reactivos con Panache y MongoDB
  - `resource`: Endpoints REST (`@Path`) que delegan en servicios
  - `service`: Lógica de negocio, flujos reactivos y transacciones

## Endpoints principales

- `GET /channels`: Listado de canales
- `GET /live`: Streaming en vivo
- `GET /pvr`: Grabaciones y reproducción
- `GET /drm`: Licencias y claves DRM
- `GET /guide`: Guía de programas

## Pruebas

- **Unitarias**: JUnit 5 y Mockito para servicios y lógica
- **Integración**: RestAssured y Quarkus Test para endpoints
- **Cobertura**: Métodos descriptivos y anotaciones `@DisplayName`

## Ejecución local

```bash
./mvnw clean package quarkus:dev
```

## Docker

- Dockerfiles en `src/main/docker` para JVM, nativo y multistage

## Configuración

- Propiedades en `src/main/resources/application.properties`
- Claves y secretos en `secret.jwk` (excluido de git)

## Buenas prácticas

- Código limpio, sin imports con comodines
- DTOs para entrada/salida en recursos
- Validación con Bean Validation (`@Valid`, `@NotNull`)
- Delegación de lógica a servicios
- Respuestas paginadas y manejo adecuado de errores

---

**qvideo** es una solución moderna y escalable para streaming de video, pensada para integrarse fácilmente en arquitecturas cloud y soportar grandes volúmenes de usuarios y contenido.
