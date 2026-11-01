# Testing and certification

## Scope and architecture

The service contains 48 production Java source files. The inventory covered its Spring MVC controllers, import/export services and targets, Excel parsing and generation, DTOs, security configuration, Feign clients, exception translation, and global exception handling. JaCoCo reports executable bytecode for 34 classes; DTOs and contracts without executable logic are exercised through their consumers.

The service has no repository or database layer. Tests therefore do not require PostgreSQL or Testcontainers. Excel documents are generated in memory, HTTP controllers and security use Spring MVC test support, and Feign collaborators are mocked so no inventory, maintenance, identity, or configuration service is required. The security slice disables Spring Cloud Config.

## Test suite

Tests are under `src/test/java/com/proseg/msvc_document_processor`. The suite has 17 test classes and reusable Excel fixtures in `testutil/ExcelFixtures`.

| Component | Scenarios certified | Test type |
|---|---|---|
| Import/export controllers | Routes, JSON and binary responses, headers, parameters, validation and error mapping | MockMvc |
| Security configuration | Anonymous, authenticated, permitted and forbidden requests by authority | Spring MVC security integration |
| Import service and target | File type/empty checks, XLS/XLSX, parse errors, row ordering, preview, confirmation and remote responses | JUnit 5 + Mockito |
| Excel parser and cell reader | Headers and aliases, blank rows/cells, dates/numbers/formulas, invalid values, schema gaps and corrupt workbooks | Unit tests using in-memory workbooks |
| Template generation | Headers, styles, column formats, freeze pane, empty and null schemas | Unit tests using generated workbooks |
| Export service and targets | Format selection, paging/limits, nested mappings, null/empty values, unavailable dependencies and Feign failures | JUnit 5 + Mockito |
| Export writers | CSV and XLSX output, headers, values and empty cells | Unit tests reading generated output |
| Security support and exceptions | JWT conversion, entry points, access-denied handling, exception translation and response bodies | Unit tests |

## Tools and commands

The service targets Java 17 and inherits Spring Boot 3.3.5. Tests use JUnit 5, Mockito, Spring Boot Test, Spring Security Test, MockMvc and JaCoCo 0.8.12.

From the repository root in PowerShell:

```powershell
Set-Location services\msvc-document-processor
.\mvnw.cmd -f ..\pom.xml -pl msvc-document-processor -am test
.\mvnw.cmd -f ..\pom.xml -pl msvc-document-processor -am verify
```

`verify` runs the reactor tests and generates:

- HTML: `target/site/jacoco/index.html`
- XML: `target/site/jacoco/jacoco.xml`

## Measured results

The final `verify` run completed successfully: 70 tests, 0 failures, 0 errors and 0 skipped.

| Metric | Covered | Total | Coverage |
|---|---:|---:|---:|
| Instructions | 3,615 | 3,654 | 98.93% |
| Lines | 667 | 679 | 98.23% |
| Branches | 294 | 315 | 93.33% |

Package-level counters (covered/total):

| Package | Instructions | Lines | Branches |
|---|---:|---:|---:|
| `client` | 210/212 (99.06%) | 42/42 (100%) | 32/40 (80%) |
| `service` | 374/376 (99.47%) | 98/99 (98.99%) | 41/42 (97.62%) |
| `excel/exception` | 4/4 (100%) | 2/2 (100%) | n/a |
| `exception` | 130/130 (100%) | 28/28 (100%) | n/a |
| root package | 8/8 (100%) | 3/3 (100%) | n/a |
| `security` | 9/9 (100%) | 3/3 (100%) | n/a |
| `excel` | 675/680 (99.26%) | 170/173 (98.27%) | 88/90 (97.78%) |
| `controller` | 125/125 (100%) | 29/29 (100%) | 6/6 (100%) |
| `export` | 105/105 (100%) | 21/21 (100%) | 7/7 (100%) |
| `export/target` | 1,258/1,285 (97.90%) | 115/121 (95.04%) | 47/54 (87.04%) |
| `export/writer` | 232/235 (98.72%) | 53/55 (96.36%) | 26/26 (100%) |
| `config` | 418/418 (100%) | 84/84 (100%) | 38/38 (100%) |
| `target` | 67/67 (100%) | 19/19 (100%) | 9/12 (75%) |

The 11 executable classes with remaining uncovered counters are:

| Class | Instructions | Lines | Branches |
|---|---:|---:|---:|
| `InventoryFeignExceptionTranslator` | 98.1% | 100% | 70.0% |
| `MaintenanceFeignExceptionTranslator` | 100% | 100% | 90.0% |
| `DocumentImportService` | 99.1% | 98.4% | 95.8% |
| `ExcelTemplateGenerator` | 98.6% | 96.2% | 100% |
| `GenericExcelParser` | 99.3% | 98.6% | 97.1% |
| `CellValueReader` | 100% | 100% | 96.9% |
| `MaintenanceRequestExportTarget` | 95.0% | 91.5% | 84.6% |
| `AssetExportTarget` | 99.7% | 97.8% | 85.7% |
| `TicketExportTarget` | 99.1% | 96.6% | 92.9% |
| `ExcelExportWriter` | 97.6% | 92.9% | 100% |
| `AssetImportTarget` | 100% | 100% | 75.0% |

The other 23 classes with JaCoCo execution counters have complete instruction, line and branch coverage. Remaining missed instruction or branch counters are attributed to these source locations:

- `client/InventoryFeignExceptionTranslator.java`: lines 29-30, 36 and 48.
- `client/MaintenanceFeignExceptionTranslator.java`: lines 30 and 36.
- `service/DocumentImportService.java`: lines 120-121.
- `excel/CellValueReader.java`: line 82.
- `excel/GenericExcelParser.java`: lines 100-101.
- `excel/ExcelTemplateGenerator.java`: lines 68-69.
- `export/target/AssetExportTarget.java`: lines 67 and 84-85.
- `export/target/MaintenanceRequestExportTarget.java`: lines 59, 63-65 and 89-90.
- `export/target/TicketExportTarget.java`: lines 67-68.
- `export/writer/ExcelExportWriter.java`: lines 49-50.
- `target/AssetImportTarget.java`: lines 32, 45 and 58.

These are uncovered alternate conditions or instructions, not excluded production classes. Coverage is below the requested 100% target; no classes were excluded to improve the reported percentage.

## Known limitations and defects

- The malformed JSON request currently reaches the generic exception handler and returns HTTP 500. The controller test records this observed behavior; a specific malformed-request response remains a production issue.
- A null confirmation request passed directly to `DocumentImportService.confirm` produces `NullPointerException`. The service test records this current behavior.
- OpenAPI documentation routes are outside the tested controller slice; the current test observes a 5xx response and does not certify a working OpenAPI endpoint.
- HTTP behavior is tested with Spring MVC slices, not a deployed service. Feign clients are not connected to real systems; there is no database layer to integrate.
- JaCoCo reports coverage but no threshold is configured. The current result does not meet the requested 100% instruction, line or branch goals.

## Certification status

**Partial certification.** The final Maven `verify` build succeeded and all 70 tests passed. The test suite certifies the listed import, export, Excel, controller, exception and security scenarios, but does not meet the 100% coverage objective and retains the observed malformed-request and null-confirmation behaviors described above.
