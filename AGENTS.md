# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## What this is

`katta-clientlib` implements the [Katta Server API](https://github.com/shift7-ch/katta-docs/blob/main/docs/introduction/OVERVIEW.md)
as [Cyberduck](https://github.com/iterate-ch/cyberduck) protocol features for [Katta Desktop](https://github.com/shift7-ch/katta-desktop).
Katta provides zero-config storage management and zero-knowledge (end-to-end encrypted) key management for teams, layered on Cryptomator's Universal Vault
Format (UVF) and Cryptomator Hub concepts.

It is a Maven multi-module build (`groupId` `cloud.katta`). Java **8** bytecode is enforced for non-test main code (`maven-enforcer-plugin`,
`maxJdkVersion 1.8`); CI compiles/tests with JDK 21. Cyberduck artifacts come from `repo.maven.cyberduck.io` (see `<repositories>` in `pom.xml`).

Related repositories that used to live here:

- [katta-admin-cli](https://github.com/shift7-ch/katta-admin-cli) — the admin CLI (`cloud.katta.cli.Katta`) for provisioning storage backends and uploading
  storage profiles, formerly the `admin-cli` module.
- [katta-compose](https://github.com/shift7-ch/katta-compose) — the Docker Compose stack (Katta Server, Keycloak, PostgreSQL, MinIO) used by the integration
  tests, formerly in the `test` module.

## Modules

| Module | Artifact              | Purpose                                                                                                                                                  |
|--------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `hub`  | `katta-clientlib-hub` | Core library: generated API client, crypto, workflows, and the `hub` / S3 Cyberduck protocols. Also holds all test fixtures. Most logic lives here.     |
| `osx`  | `katta-clientlib-osx` | macOS `NSAlert`-based UI controllers (`ch.cyberduck.binding`) implementing the device-setup / first-login prompts. Depends on `katta-clientlib-hub`. |

## Build & test commands

```bash
# Full build incl. integration tests (spins up Docker containers via Testcontainers)
mvn --batch-mode verify -U

# Unit tests only — skips integration tests
mvn clean verify -DskipITs

# Single unit test class / method
mvn -pl hub test -Dtest=UserKeysTest
mvn -pl hub test -Dtest=UserKeysTest#recoverUserKeyPair

# Single integration test with debug logging
mvn clean verify -Dit.test=cloud.katta.workflows.HubWorkflowGroupTest \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dlog4j.configurationFile=./hub/src/test/resources/log4j-test.xml
```

### Unit vs. integration tests

- Tests are split by JUnit tag, not by class name suffix. Surefire runs everything except `<excludedGroups>hub</excludedGroups>`; Failsafe includes
  `**/*.java` and runs only `<groups>hub</groups>` in the `integration-test` phase.
- `@HubIntegrationTest` (`@Tag("hub")`) marks integration tests. `AbstractHubTest` carries it, and the `AbstractHub*Test` bases in `hub/src/test/.../workflows/`
  extend it, so many `*Test` classes are integration tests — check the base class before assuming a `*Test` is a pure unit test.
- Integration tests start the full Keycloak + MinIO + Katta Server stack through `HubTestSetupDockerExtension` / Testcontainers (see below), so Docker must be
  running and runs are slow.

## Architecture

### Generated API client — do not hand-edit

`hub/src/main/resources/openapi.json` is the checked-in OpenAPI spec. During `process-sources` the
`openapi-generator-maven-plugin` (generator `java`, library `jersey2`) generates `cloud.katta.client`,
`cloud.katta.client.api`, `cloud.katta.client.model` into `hub/target/generated-sources/openapi`. **This generated code is not committed.** The plugin output
directory is the `hub` module base directory, so generator metadata also lands in `hub/.openapi-generator/` — do not commit it either. To pick up server
API changes, replace `openapi.json` (from the server's `/q/openapi.json`) and rebuild. `HubApiClient` (committed) subclasses the generated `ApiClient` to wire in
Cyberduck's HTTP stack, timeouts and user-agent. Custom Jackson deserializers for polymorphic DTOs live in `cloud.katta.protocols.hub.serializer`.

### Crypto (`cloud.katta.crypto`)

Zero-knowledge key hierarchy, mirroring the Cryptomator Hub / UVF TypeScript implementation:

- `DeviceKeys` (per-device EC keypair) decrypts → `UserKeys` (per-user EC keypair, stored server-side as JWE) → decrypts vault **member key** (AES); vault
  **owners** additionally get the **recovery key**.
- `JWE` / `JWT` / `KeyHelper` wrap Nimbus JOSE. `*Payload` classes are typed JWE/JWT payload bodies.
- `cloud.katta.crypto.uvf` — `UVFMetadataPayload` (`vault.uvf`), `UVFAccessTokenPayload`, `HubVaultKeys`. These are deliberate counterparts of specific files in
  `katta-server` / the UVF spec; keep them in sync (see class Javadoc links).
- `cloud.katta.crypto.wot` — Web-of-Trust signature verification (`WoT`, `SignedKeys`).

### Workflows (`cloud.katta.workflows`)

`*Service` interface + `*ServiceImpl` pairs orchestrating multi-step server interactions, each taking a `HubSession`:

- `UserKeysService` — first login / device pairing: get-or-create the user keypair, setup-code handling.
- `DeviceKeysService` — device registration.
- `GrantAccessService` — grant a user access to a vault (re-encrypt member key for their user key); also driven automatically by
  `HubGrantAccessSchedulerService` for pending access requests.
- `VaultService` — create vaults, fetch the user-specific vault access token and encrypted `vault.uvf` metadata.
- `WoTService` — sign/verify other users' keys.

### Protocols (`cloud.katta.protocols`)

- `protocols.hub` — the `hub` Cyberduck `Protocol` (`@AutoService(Protocol.class)`, registered via
  `google-auto-service`). `HubSession` is the entry point: authenticates via OAuth (Cyberduck manages tokens), pairs the device, caches `UserKeys` in an
  `ExpiringObjectHolder`, and exposes vault listing/registry/metadata features (`HubVaultRegistry`, `HubUVFVaultProvider`, `HubVaultListService`,
  `HubStorageProfile`, …).
- `protocols.s3` — `STSChainedAssumeRoleRequestInterceptor` implements AWS role-chaining / token exchange for the
  `S3` and `S3STS` Katta modes (temporary credentials from an OIDC access token via STS).

## Integration test environment (Docker Compose)

The stack is not defined in this repository. `hub/src/test/resources/compose.yaml` only `include`s the compose file of
[katta-compose](https://github.com/shift7-ch/katta-compose) by Git URL (`…/katta-compose.git#main`); Docker Compose fetches it on first use. To test against a
local katta-compose checkout, replace the Git URL with the absolute path to its `compose.yaml` (do not commit that change).

Project-specific inputs stay in `hub/src/test/resources`:

- `keycloak/cryptomator-realm.json` — Keycloak realm with the test users (listed in `README.md`), passed as `KEYCLOAK_REALM_FILE`.
- `setup/` — storage-profile and bucket-policy JSON (`aws_static`, `aws_sts`, `minio_static`, `minio_sts`), passed as `SETUP_DIR`.
- `.local.env` (profile `local`, fully local) and `.chipotle.env` (profile `hybrid`, Keycloak/MinIO on `testing.katta.cloud` + AWS S3; CI writes it from the
  `HYBRID_ENV` secret).

`cloud.katta.testsetup.KattaCompose` builds the Testcontainers `ComposeContainer` from these (loads the env file, sets `KEYCLOAK_REALM_FILE` / `SETUP_DIR`,
selects the profile, waits for the `hub` service healthcheck). `HubTestSetupDockerExtension` wraps it in `Local*` / `HybridTesting*` variants, each with
`KeepRunning` and `AlreadyRunning` modes to skip teardown or setup. Commands to start the stack manually are in `README.md`; profiles and endpoints are documented
in katta-compose.

## Conventions

- `.editorconfig` is authoritative: 4-space indent, LF, final newline, max line length 160, UTF-8. IntelliJ formatting keys are pinned there.
- Every source file starts with the `Copyright (c) <year> shift7 GmbH. All rights reserved.` header.
- CI: `build.yml` (pull requests) and `integration.yml` (push to `main`, nightly) both run `mvn verify` including integration tests; `deploy.yml` publishes
  snapshots from `main`.
- Releases are cut with `maven-release-plugin` (`[maven-release-plugin] prepare release …` commits). Artifacts deploy (`<distributionManagement>`) to
  `s3://repo-maven-shift7` (`releases/` and `snapshots/`); the
  `repo.maven.cyberduck.io` entries in `<repositories>` are only for *consuming* Cyberduck dependencies. Dependency bumps come through Dependabot PRs.
