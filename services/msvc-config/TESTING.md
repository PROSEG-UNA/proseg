# Testing and certification

## Architecture and inventory

`msvc-config` targets Java 17 and inherits Spring Boot 3.3.5 and Spring Cloud 2023.0.3 from `services/pom.xml`. It contains one production Java class, `MsvcConfigApplication`, which enables Spring Cloud Config Server. The configured repository is the native classpath backend at `classpath:/configurations`; there is no Git backend, application-specific controller, or configured remote repository.

The production configuration store has 16 YAML files: default and `prod` configurations for `msvc-archive`, `msvc-auth`, `msvc-document-processor`, `msvc-email`, `msvc-gateway`, `msvc-inventory`, `msvc-maintenance`, and `msvc-transport`. The Config Server runtime has a default `native` profile and `application-docker.yml` for Docker runtime configuration. There are no production `.properties` files or multi-document YAML files.

## Test matrix

The suite contains two test classes and nine test cases:

| Component | Scenarios | Test type | Result |
|---|---|---|---|
| Config Server HTTP contract | Starts the real Config Server on a random local port; resolves all eight application names under `default` and `prod`; verifies app name and property sources | Spring Boot integration test | Passed |
| Profile and precedence behavior | Confirms shared `application.yml`, service-specific configuration, production-specific overrides, and fallback for unknown applications/profiles | Spring Boot integration test with synthetic classpath fixtures | Passed |
| Supported formats | Reads a `.properties` fixture and a multi-document YAML fixture with a profile-activated document | Spring Boot integration test | Passed |
| Invalid configuration | Malformed YAML results in a server error; the HTTP body does not contain the synthetic fixture value | Spring Boot integration test | Passed |
| Access and path handling | Confirms the configuration endpoint responds without credentials and `/actuator/health` is resolved as a Config Server environment request (`application=actuator`, `profile=health`) | Spring Boot integration test | Passed |
| Application bootstrap | Verifies `main` delegates to `SpringApplication.run` | JUnit 5 + Mockito static mock | Passed |

Test fixtures are under `src/test/resources/test-configurations`. They use synthetic values and are isolated from the production `classpath:/configurations` resources. Tests use no external services, credentials, or remote repositories.

Labels/branches are not tested as Git revisions because the configured repository is native classpath files, not Git. No service-specific behavior for branch selection is configured. Unknown names/profiles are tested for native fallback behavior.

## Commands and reports

From `services` in PowerShell:

```powershell
.\msvc-config\mvnw.cmd -f .\pom.xml -pl msvc-config -am clean verify
```

The verified run completed with `BUILD SUCCESS` for the reactor. JaCoCo reports are generated at:

- HTML: `services/msvc-config/target/site/jacoco/index.html`
- XML: `services/msvc-config/target/site/jacoco/jacoco.xml`

## Verified results

The final clean verification ran **9 tests: 0 failures, 0 errors, 0 skipped**.

| JaCoCo counter | Covered | Total | Coverage |
|---|---:|---:|---:|
| Instructions | 8 | 8 | 100% |
| Lines | 3 | 3 | 100% |
| Branches | n/a | n/a | No branch counters in production Java code |
| Methods | 2 | 2 | 100% |
| Classes | 1 | 1 | 100% |

The sole production class, `MsvcConfigApplication`, has complete instruction and line coverage. This Java coverage is not a substitute for the HTTP/profile contract tests above. No production class was excluded from JaCoCo.

## Security and operational risks

- The production POM does not declare Spring Security or an authentication mechanism for Config Server. The integration test confirms that an unauthenticated request can retrieve a service environment. Any network-reachable caller may therefore inspect configuration values and internal service topology. Network isolation or an explicit access-control design should be confirmed for each deployment; no production security behavior was changed in this testing task.
- A value-free scan of the 16 production YAML files found 24 fields with sensitive-looking keys: 23 use environment-variable placeholders and one has an empty default; no literal sensitive values were found. Placeholders are returned as configuration values by Config Server and require secure environment injection by consumers.
- A malformed-YAML test confirms a 5xx response without fixture content in the HTTP body. The Config Server logs the YAML parser exception, including the offending source line; avoid embedding secrets in configuration lines because malformed-file diagnostics may expose source content to logs.
- `/actuator/health` did not return an Actuator health representation in the tested HTTP context; it resolved to the Config Server application's `actuator` profile `health`. No health-check contract is certified at this path.
- The Docker runtime configuration was inspected but not launched in a container. No real deployment network boundaries, secret stores, remote configuration, or consumer bootstraps were available for certification.

## Certification status

**CERTIFICACIÓN PARCIAL.** The Config Server booted locally and all nine tests passed. Java instruction and line coverage is 100%, with no branch counters applicable. Functional checks cover the configured native classpath sources, both production profiles, precedence, formats, and malformed input. Certification remains partial because configuration retrieval is unauthenticated, deployment network boundaries are unverified, and no remote deployment or consumer integration was exercised.
