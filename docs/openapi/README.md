# OpenAPI Contract

This directory stores the tracked OpenAPI contract for `git-ranker`.

## Files

- `openapi.json`: generated baseline contract for the public `/api/v1/**` API surface

## Regeneration

Run the following command from the repository root:

```bash
./gradlew generateOpenApiSpec
```

The task runs the OpenAPI test slice with the `openapi` profile and writes the latest contract to `docs/openapi/openapi.json`.

## Runtime Endpoints

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

## Auth Notes

- Protected endpoints accept either `Authorization: Bearer <JWT>` or the `accessToken` cookie.
- `/api/v1/auth/refresh` uses the `refreshToken` cookie.
- The initial GitHub OAuth2 login flow is handled by Spring Security outside `/api/v1/**`.
