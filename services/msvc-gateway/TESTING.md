# msvc-gateway testing and certification

## Scope and architecture

The Gateway is a Java 17 Spring Boot 3.3.5 application using Spring Cloud Gateway WebFlux, Reactor Netty, Spring Security's reactive OAuth2 resource server, and JWTs. Its production source contains eight Java classes. Routing is configured in `src/main/resources/application.yml`; no Eureka discovery, Git source, or resilience filter is configured there.

The suite uses JUnit 5, AssertJ, Spring Boot Test, `WebTestClient`, and a local Reactor Netty HTTP backend. Integration tests start the real Gateway on a random port and use an RSA key generated only for tests. Config Server is disabled for tests; all service destinations and refresh requests point to the local backend. No real credentials or external services are used.

## Test inventory and route matrix

The suite contains four test classes and 22 cases: 9 unit tests and 13 Spring integration tests.

| Component / route | Contract exercised | Result / limitation |
|---|---|---|
| `/api/auth/**`, `/api/user/**`, `/api/role/**` | Route definitions; auth login is forwarded to the local backend | Passed |
| `/api/email/**` | Route definition and `StripPrefix=2`; request with a valid test JWT | Gateway returns 403 before routing because `SecurityConfig` does not permit or authenticate this path. Prefix rewriting cannot be exercised end-to-end until the intended access policy is decided. |
| `/api/v1/inventory/**` | Route definition and anonymous forwarding | Passed |
| `/api/v1/archive/**` | Anonymous forwarding, method/body/query/header propagation, backend 404/503 forwarding, JWT signature and expiry rejection when a token is supplied, refresh success/failure | Passed |
| `/api/v1/transport/**` | Route definition and anonymous forwarding | Passed |
| `/api/v1/document-processor/**` | Route definition and anonymous forwarding | Passed |
| `/api/v1/maintenance/**` | Route definition and anonymous forwarding; path prefix preserved | Passed |
| `/actuator/**` | Route definition; health endpoint returns `UP` without authentication | Health passed. The configured route destination is `localhost:8081`; gateway forwarding behavior is not separately exercised because the test server uses a random port. |
| Unknown path | Anonymous request is rejected by the security chain | Passed; the observed response is 401 rather than a route-level 404. |

Configured routes have no HTTP method predicates. An integration request verifies that POST, the query string, request body, a request header, backend response headers, status codes, and response bodies survive forwarding. The email route's actual forwarding and path rewrite remain unverified due to its authorization rejection.

## Authentication, authorization, filters, and CORS

- Unit tests cover bearer-header precedence over the `auth_token` cookie, cookie fallback, missing/blank tokens, and the converter's exact public-path matching behavior.
- Integration tests use a test-only generated RSA key and the actual resource-server filter chain to accept a signed token and reject malformed, tampered, and expired tokens. The test decoder validates standard timestamps; test tokens do not claim a production issuer or audience, so issuer/audience validation is not certified.
- The `ReactiveKeycloakJwtConverter` tests cover role extraction, internal-role filtering, missing realm roles, and absent role claims.
- Handler tests check 401/403 JSON responses and that test-only sensitive marker text is not emitted.
- Integration tests exercise successful token refresh and refresh rejection/expired-cookie behavior through a local auth endpoint.
- CORS integration checks an allowed development origin, credential permission, allowed methods, and that an unlisted origin receives no allow-origin header.
- `SecurityConfig` explicitly permits `/api/auth/**`, `/api/user/**`, `/api/role/**`, inventory, invitations, archive, transport, maintenance, and document processor paths. Tests confirm anonymous archive/inventory forwarding. These rules mean the Gateway is not the authorization boundary for those service paths; each destination must enforce its own business authorization.
- Email is not covered by the permit-all path list and an authenticated test request receives 403. This may block the configured email route; the intended policy needs confirmation. No production authorization rule was changed.
- The test suite does not certify external Keycloak realm behavior, production issuer/audience settings, forged forwarding-header behavior, or the destination services' authorization.

## Resilience and external dependencies

No retry, timeout, circuit breaker, rate limiter, bulkhead, fallback, or Eureka discovery configuration was found in the Gateway's route configuration. The integration suite verifies backend 404 and 503 response propagation, but does not simulate connection refusal or timeout; those mechanisms are not configured in this service. Config Server, Eureka, Keycloak, and other microservices are not contacted during tests.

## Commands and reports

From `services` in PowerShell:

```powershell
.\msvc-gateway\mvnw.cmd -f .\pom.xml -pl msvc-gateway -am clean verify
```

JaCoCo 0.8.12 writes:

- HTML: `services/msvc-gateway/target/site/jacoco/index.html`
- XML: `services/msvc-gateway/target/site/jacoco/jacoco.xml`

The clean verification completed with `BUILD SUCCESS`. Surefire reported 22 tests, 0 failures, 0 errors, and 0 skipped. The reactor also built `msvc-common`, which has no tests.

## JaCoCo results

| Counter | Covered | Missed | Coverage |
|---|---:|---:|---:|
| Instructions | 781 | 64 | 92.43% |
| Lines | 170 | 19 | 89.95% |
| Branches | 44 | 16 | 73.33% |
| Methods | 39 | 3 | 92.86% |
| Classes | 8 | 0 | 100% |

Coverage includes all eight production classes; none were excluded. The remaining misses are concentrated in application startup, optional/error branches in `TokenRefreshFilter`, and conditional paths in the JWT configuration classes. Java coverage does not measure YAML route/security behavior, which is covered separately only to the extent described in the route and security matrices. The requested 100% instruction, line, and branch goal was not met.

## Risks and limitations

- Several API route families are explicitly anonymous at the Gateway. The test verifies this for archive and inventory; destination services must independently enforce authorization for protected operations.
- `/api/email/**` is configured as a route but is rejected by the current Gateway security chain (403), so its functional forwarding contract is not certified.
- The test uses a locally generated signing key and standard timestamp validation; it does not connect to Keycloak or validate production issuer/audience/realm configuration.
- Health is tested directly on the Gateway. The separate `gateway-actuator` route points to `localhost:8081`, which is not the random test port and was therefore not called through the proxy.
- Connection failures, network timeouts, production service availability, TLS, forwarded-header trust, and real deployment configuration were not exercised.
- No production behavior was modified. The test result is evidence for the local test profile, not a deployment security certification.

## Certification status

**CERTIFICACIÓN PARCIAL.** The clean Maven verification passed all 22 tests, and JaCoCo reports 100% class coverage. The requested instruction/line/branch goals remain incomplete; email routing is blocked by the current security rules, and issuer/audience, deployment integrations, connection failures, and production network behavior remain unverified.
