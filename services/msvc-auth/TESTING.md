# msvc-auth testing and certification

## Scope and inventory

This certification covers `msvc-auth` only. Production inventory: 59 Java source files: application root 1, client 1, config 8, controller 3, DTO 18, entity 4, exception 10, repository 4, scheduler 1, security 1, and service 8.

The HTTP surface contains 29 controller mappings:

- Auth: health, current user, login, logout, token refresh, forgot/reset/change password, and registration.
- User: list/create, Keycloak user lookup and batch lookup, roles, statuses, approval, role assignment/removal, account activation, invitation resend/info, and profile image update.
- Role: base/composite role listings, role composites, create/update/delete, and users by role.

Persistence consists of four JPA entities and four repositories: user approval status, invitation tokens, password policies, and password-reset tokens. Other production integrations are Keycloak Admin/Auth HTTP, Cloudflare Turnstile, the gateway's maintenance-company endpoint, and Kafka events/topics.

## Test matrix

**Certification status: PARTIAL.** An independent read-only security review identified the identity-confusion defect described below. It was fixed in the current worktree: `/api/auth/me` now selects the profile using the authenticated principal, and a regression test verifies that a conflicting cookie cannot change the selected user. The coverage results remain below the requested 100% objective, and the token-subject parsing helper still does not validate JWT signatures.

The suite has 17 test classes and 79 test cases:

| Area | Coverage |
| --- | --- |
| MockMvc and security | Exercises all controller routes, DTO validation, public and privileged routes, unauthorized/forbidden outcomes, cookie behavior, responses, and service delegation. The relevant `/me` request uses a generated-key RS256-signed bearer token, a deliberately conflicting unsigned cookie, and the real resource-server decoder. The test does not use MockMvc's `jwt()` postprocessor. |
| HTTP clients | `MockRestServiceServer` fixtures cover Keycloak token/admin requests, Turnstile verification/retries, and the maintenance-company gateway client. All credentials and tokens are test fixtures; no real service is called. |
| Services and scheduler | Covers password policy/reset and invitation workflows, account approval, role/company preconditions, image metadata, Keycloak role/user behavior, retry/failure handling, and expiration notifications. Kafka is mocked. |
| JPA/repositories | H2 integration tests persist each entity and exercise derived lookups, expiration windows, and JPQL token invalidation queries. |
| Configuration and exceptions | Checks resolver/converter and error-handler behavior, Kafka topic/producer configuration, REST configuration, exception factories, and role-service delegation. |

No real credentials or external service calls are used. H2 is suitable for the exercised repository layer: the custom repository updates are standard JPQL and no PostgreSQL-specific SQL or operators were found. This is not a PostgreSQL certification; production PostgreSQL dialect/driver behavior and concurrency characteristics still require a PostgreSQL-backed environment. Kafka broker behavior and real Keycloak/Cloudflare/gateway contracts are likewise not exercised.

## Security finding - identity confusion and unsigned JWT payload parsing

The independent read-only security review reported that the reviewed `AuthController.getCurrentUser` revision selected the requested user from the `auth_token` cookie, while Spring's bearer-token resolver could authenticate a different valid `Authorization` bearer token. `KeycloakAuthService.extractUserIdFromToken` only splits the token and base64url-decodes its payload; it does not validate the JWT signature, issuer, audience, expiry, or other claims. This allowed the reviewed path to use an attacker-controlled cookie subject for `/api/auth/me` despite the request being authenticated as another principal.

The current worktree version of `getCurrentUser` uses `authentication.getName()` as the lookup key. `AuthMvcSecurityTest.currentUserLookupUsesAuthenticatedSubjectInsteadOfConflictingCookie` sends a genuinely signed test bearer with subject `signed-test-user` and a different unsigned cookie containing subject `forged-user`; it asserts only the authenticated subject's profile is requested and returned. The bearer signature is verified by the real resource-server decoder using the generated test key. The test does not use MockMvc's `jwt()` postprocessor and makes no signature-validation claim about the cookie.

- `KeycloakAuthServiceTest.extractsSubjectFromJwtShapedInputWithoutValidatingHeaderOrSignature` separately passes JWT-shaped input with `alg: none` and an invalid signature placeholder and confirms the parser returns the payload subject. This characterizes unsafe parsing; it is not an accepted authentication contract and is not evidence that JWT signatures are validated.

The principal-based production change is included in this worktree. The regression proves the current endpoint behavior, not that the change has been merged or deployed. `extractUserIdFromToken` remains used by the login flow to read the subject from Keycloak's token response; it remains a payload parser, not a JWT validator. Never interpret the JWT-shaped payload-parser test as signature validation.

## Execution

Run from `services` in PowerShell:

```powershell
.\msvc-auth\mvnw.cmd -f .\pom.xml -pl msvc-auth -am clean verify
```

Verified result: **BUILD SUCCESS** for `services`, `msvc-common`, and `msvc-auth`; **79 tests, 0 failures, 0 errors, 0 skipped**. JaCoCo ran in `verify` and generated:

- `target/site/jacoco/index.html`
- `target/site/jacoco/jacoco.xml`

The `msvc-common` dependency is built by the requested `-am` reactor command and has no tests. The test contexts disable Spring Cloud Config; service interactions use local mocks.

## JaCoCo results

Measured after the successful reactor `clean verify` (including the security characterization tests):

| Counter | Covered | Total | Coverage |
| --- | ---: | ---: | ---: |
| Instructions | 6,297 | 7,394 | 85.16% |
| Lines | 1,471 | 1,683 | 87.40% |
| Branches | 269 | 384 | 70.05% |
| Methods | 235 | 249 | 94.38% |
| Classes | 34 | 37 | 91.89% |

Package coverage (instructions / lines / branches, covered / total):

| Package | Instructions | Lines | Branches |
| --- | ---: | ---: | ---: |
| `config` | 776 / 781 | 179 / 179 | 38 / 44 |
| `entity` | 27 / 27 | 5 / 5 | no branches |
| `security` | 0 / 9 | 0 / 3 | no branches |
| application root | 3 / 8 | 1 / 3 | no branches |
| `client` | 85 / 85 | 20 / 20 | 6 / 6 |
| `controller` | 667 / 688 | 171 / 176 | 16 / 26 |
| `exception` | 413 / 421 | 84 / 85 | 8 / 12 |
| `scheduler` | 90 / 90 | 31 / 31 | 4 / 4 |
| `dto` | 3 / 3 | 1 / 1 | no branches |
| `service` | 4,233 / 5,282 | 979 / 1,180 | 197 / 292 |
| `repository` | no executable repository interface lines | no executable lines | no executable lines |

The 100% target was not reached. Remaining uncovered production lines/branches are concentrated in:

| Production class | Uncovered lines | Uncovered branches |
| --- | ---: | ---: |
| `MsvcAuthApplication` | 2 | 0 |
| `Privileges$Role`, `Privileges$User`, `Privileges$Invitation` | 1 each | 0 |
| `AuthController` | 5 | 10 |
| `GlobalExceptionHandler` | 1 | 4 |
| `UserService` | 22 | 15 |
| `KeycloakAdminService` | 145 | 61 |
| `KeycloakAuthService` | 21 | 14 |
| `PasswordResetService` | 8 | 0 |
| `TurnstileService` | 5 | 5 |

The privilege classes are constant-only holders. The application entry point is not launched because doing so would bring up the service's runtime infrastructure rather than test the `main` delegation. Remaining service misses are primarily unexercised HTTP/error and edge branches; the JaCoCo XML contains per-line detail. Repository interfaces have no executable lines for JaCoCo to measure.

## Observed behavior and limits

- Invalid registration CAPTCHA currently throws a generic runtime exception and returns HTTP 500. The test records the existing behavior; production behavior was not changed.
- Role update/delete handlers call `ApiResponseBuilder.noContent`, which returns HTTP 200 with a response body, not 204. This helper is in `msvc-common`, outside the permitted change scope; endpoint tests assert the observed 200.
- H2 tests validate mappings and JPQL repository queries, not production PostgreSQL operation or transactional race behavior.
- The `/api/auth/me` identity binding change is a production behavior change authorized as the fix for the security finding. Other observed behaviors above remain unchanged.
