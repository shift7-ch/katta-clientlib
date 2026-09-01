# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## What this is

`katta-clientlib` implements the [Katta Server API](https://github.com/shift7-ch/katta-docs/blob/main/OVERVIEW.md)
as [Cyberduck](https://github.com/iterate-ch/cyberduck) protocol features for the Katta desktop client, plus an admin CLI for provisioning storage backends.
Katta provides zero-config storage management and zero-knowledge (end-to-end encrypted) key management for teams, layered on Cryptomator's Universal Vault
Format (UVF) and Cryptomator Hub concepts.

It is a Maven multi-module build (`groupId` `cloud.katta`). Java **8** bytecode is enforced for non-test main code (`maven-enforcer-plugin`,
`maxJdkVersion 1.8`); CI compiles/tests with JDK 21, and the CLI native image uses GraalVM (JDK 25). Cyberduck artifacts come from `repo.maven.cyberduck.io`
(see `<repositories>` in `pom.xml`).

## Modules

| Module      | Artifact                | Purpose                                                                                                                                                                                                                                   |
|-------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test`      | `katta-clientlib-tests` | Shared test **resources only** (docker-compose stack, Keycloak realm, `.env` files, storage-profile JSON). Packaged as a `tests` classifier jar and unpacked into the other modules' `target/test-classes` via `maven-dependency-plugin`. |
| `hub`       | `katta-clientlib-hub`   | Core library: generated API client, crypto, workflows, and the `hub` / S3 Cyberduck protocols. Most logic lives here.                                                                                                                     |
| `osx`       | `katta-clientlib-osx`   | macOS `NSAlert`-based UI controllers (`ch.cyberduck.binding`) implementing the device-setup / first-login prompts.                                                                                                                        |
| `admin-cli` | `katta-admin-cli`       | picocli CLI (`cloud.katta.cli.Katta`) to configure a Katta Server + its S3/MinIO/AWS-STS storage backends. Builds to a GraalVM native image.                                                                                              |

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

# Build the admin CLI native image
mvn --batch-mode install -pl admin-cli -am -DskipTests
mvn --batch-mode verify -pl admin-cli -Pnative
```

### Unit vs. integration tests

- **Unit tests** = Surefire, class/file suffix `*Test`. `surefire` config sets `<excludedGroups>hub</excludedGroups>`.
- **Integration tests** = Failsafe, suffix `*IT`, run in the `integration-test` phase with `<groups>hub,cli</groups>`. They are gated by JUnit tags:
  `@HubIntegrationTest` (`@Tag("hub")`) and `@CLIIntegrationTest` (`@Tag("cli")`). A class annotated `@HubIntegrationTest` (e.g. `AbstractHubTest`) starts the
  full Keycloak + MinIO + Katta Server stack through `HubTestSetupDockerExtension` / Testcontainers, so Docker must be running and integration runs are slow.
- Note some integration behaviour is also covered by `*Test` classes extending `AbstractHub*Test` bases in
  `hub/src/test/.../workflows/` — check the base class before assuming a `*Test` is pure unit.

## Architecture

### Generated API client — do not hand-edit

`hub/src/main/resources/openapi.json` is the checked-in OpenAPI spec. During `process-sources` the
`openapi-generator-maven-plugin` (generator `java`, library `jersey2`) generates `cloud.katta.client`,
`cloud.katta.client.api`, `cloud.katta.client.model` into `hub/target/generated-sources/openapi`. **This generated code is not committed.** To pick up server
API changes, replace `openapi.json` (from the server's
`/q/openapi.json`) and rebuild. `HubApiClient` (committed) subclasses the generated `ApiClient` to wire in Cyberduck's HTTP stack, timeouts and user-agent.
Custom Jackson deserializers for polymorphic DTOs live in
`cloud.katta.protocols.hub.serializer`.

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

### admin-cli

picocli command tree rooted at `cloud.katta.cli.Katta`. Subcommands: `setup` (provision AWS/MinIO STS: IAM identity provider + roles), `storageprofile` (upload
storage-profile config to a Katta Server: `aws sts`,
`aws static`, `s3 static`, `minio sts`), `login`, `completion`. See `admin-cli/README.md` for full option docs. GraalVM native-image reachability metadata is
captured under `admin-cli/src/main/resources/META-INF/native-image`.

## Local stack (Docker Compose)

Compose file: `test/src/test/resources/docker-compose-hub-keycloak-minio.yml`. Profiles: `local` (fully local),
`hybrid` (integration tests against deployed `testing.katta.cloud` + real AWS S3), `demo` (local + deploys MinIO storage profiles). Env files: `.local.env`,
`.chipotle.env`. Key local endpoints: Katta Web `:8280`, Keycloak
`:8380` / `:8443`, MinIO console `:9101`, Swagger UI `http://localhost:8280/q/swagger-ui/`. Test users and the full command lines are in `README.md`.

## Conventions

- `.editorconfig` is authoritative: 4-space indent, LF, final newline, max line length 160, UTF-8. IntelliJ formatting keys are pinned there.
- Every source file starts with the `Copyright (c) <year> shift7 GmbH. All rights reserved.` header.
- Releases are cut with `maven-release-plugin` (`[maven-release-plugin] prepare release …` commits). Artifacts deploy (`<distributionManagement>`) to
  `s3://repo-maven-shift7` (`releases/` and `snapshots/`); the
  `repo.maven.cyberduck.io` entries in `<repositories>` are only for *consuming* Cyberduck dependencies. Dependency bumps come through Dependabot PRs.
