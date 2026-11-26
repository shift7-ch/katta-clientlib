# Katta Client Library

[![CI Build](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml)
[![Integration Tests](https://github.com/shift7-ch/katta-clientlib/actions/workflows/integration.yml/badge.svg)](https://github.com/shift7-ch/katta-clientlib/actions/workflows/integration.yml)


> [Katta](https://katta.cloud/): transform your S3 storage into a secure, team-friendly workspace with client-side encryption.

This library implements the [Katta Server API](https://github.com/shift7-ch/katta-docs/blob/main/docs/introduction/OVERVIEW.md)
as [Cyberduck](https://github.com/iterate-ch/cyberduck) protocol features for [Katta Desktop](https://github.com/shift7-ch/katta-desktop).

Features:

* Client code is generated for Katta Server API from the [OpenAPI specification](hub/src/main/resources/openapi.json).
* Implementations for device setup, retrieval of available storage profiles and creation of vaults in UVF format.
* Extensions for the OIDC authentication flow using token exchange and AWS role chaining
  for [Katta S3 Storage Access](https://github.com/shift7-ch/katta-docs/blob/main/docs/setup/SERVER_SETUP.md#storage-provider-setup)).

This is a Maven multi-module project:

| Module                             | Artifact                | Description                                                                                                                                                                                                                                              |
|------------------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`hub`](hub)                       | `katta-clientlib-hub`   | Core client library. Contains the OpenAPI-generated Katta Server API client, the Cyberduck `hub` protocol, the workflow services (device and user key management, vault creation, access grants, Web of Trust) and the S3/STS storage-access extensions. |
| [`osx`](osx)                       | `katta-clientlib-osx`   | macOS integration. Cocoa binding controllers (`ch.cyberduck:binding`) that wire the workflows into the Cyberduck desktop UI, e.g. first-login and device-setup prompts.                                                                                  |


## Development Setup

### Unit tests

Run unit tests only:

```shell
mvn clean verify -DskipITs
```

### Debug logging

To run a single integration test with debug logging, use

```shell
mvn clean verify -Dit.test=cloud.katta.workflows.HubWorkflowGroupTest \\
 -Dfailsafe.failIfNoSpecifiedTests=false -Dlog4j.configurationFile=./hub/src/test/resources/log4j-test.xml
```

### Docker Compose environment

Integration tests start a Docker Compose scenario of the deployment examples in
[katta-server](https://github.com/shift7-ch/katta-server/tree/develop/deploy/compose), one include file per scenario,
included with its Git URL:

| Scenario | Include file | Env file | katta-server directory |
|---|---|---|---|
| `local` | [`compose-local.yaml`](hub/src/test/resources/compose-local.yaml) | [`.local.env`](hub/src/test/resources/.local.env) | `deploy/compose/local` |
| `hybrid` | [`compose-hybrid.yaml`](hub/src/test/resources/compose-hybrid.yaml) | [`.chipotle.env`](hub/src/test/resources/.chipotle.env) | `deploy/compose/hybrid` |

Docker Compose fetches the referenced commit on first use. To run integration tests with a local checkout of
katta-server instead, replace the Git URL with the absolute path to `compose.yaml` in the scenario directory of the
checkout.

## Integration Test Environment

The `local` scenario runs Katta Server, Keycloak, PostgreSQL and MinIO, using the storage profiles and env files of this
project in [`hub/src/test/resources`](hub/src/test/resources); the MinIO policies are the ones of katta-server.
katta-server renders the Keycloak realm from its Helm chart.

To start the environment of the integration tests yourself, use

```bash
docker compose -f hub/src/test/resources/compose-local.yaml --env-file hub/src/test/resources/.local.env up --wait
docker compose -f hub/src/test/resources/compose-local.yaml --env-file hub/src/test/resources/.local.env down
```

The endpoints `hub.localhost`, `keycloak.localhost` and `minio.localhost` resolve to the loopback address on the host,
and `minio.localhost` also to the MinIO container inside the Docker network. The JVM running the tests uses the system
resolver, which on macOS does not resolve subdomains of `localhost`. Add them to `/etc/hosts`:

```
127.0.0.1 hub.localhost keycloak.localhost minio.localhost
```

The `hybrid` scenario runs Katta Server and PostgreSQL against Keycloak and MinIO on `testing.katta.cloud` and AWS S3,
configured by [`.chipotle.env`](hub/src/test/resources/.chipotle.env). CI writes its values from a repository secret.
[`compose-hybrid.env`](hub/src/test/resources/compose-hybrid.env) maps its variables to the ones `deploy/compose/hybrid`
reads.

### Provisioned Users

#### Keycloak

The realm of the `local` scenario provisions the administrator `admin` with password `admin`. Before the tests of the
`local` scenario, [`KattaTestRealm`](hub/src/test/java/cloud/katta/testsetup/KattaTestRealm.java) adds the test configuration:

- It enables direct access grants in client `cryptomator` for the password grant of the integration tests.
- It creates the user `HUB_USER` with password `HUB_PASSWORD` of the env file (`alice` with password `asd`) and the roles
  `user` and `create-vaults` using the API of Katta Server.

The setup is idempotent. When you start the environment yourself with the commands above, run the integration tests with
`HubTestSetupDockerExtension.LocalAlreadyRunning`, which adds the test configuration to the running environment.

#### MinIO
MinIO provisions the root user `minioadmin` with password `minioadmin`, and the user `testuser` with password `top-secret`
for static storage access.

#### Further Architecture Documentation

- [`docs/vault_data_model.md`](docs/vault_data_model.md) — class diagram tracing foreign-key relationships across storage profiles, the `/vault` API, the vault
  `.uvf` JWE metadata, and the Hub bookmark.
- [`docs/s3_interactions.md`](docs/s3_interactions.md) — sequence diagrams of every S3/STS touchpoint across the desktop client, Hub backend, and Hub web
  frontend, for both S3-STS and S3STATIC storage profiles.
- [`docs/account_key_interactions.md`](docs/account_key_interactions.md) — creation, recovery, and rotation of the per-user Account Key across the desktop
  client and Hub.
- [`docs/recovery_key_interactions.md`](docs/recovery_key_interactions.md) — creation and recovery of the per-vault, word-encoded Recovery Key, a
  web-frontend-only feature distinct from the Account Key.
- [`docs/access_grant_data_model_interactions.md`](docs/access_grant_data_model_interactions.md) — manual vs. automatic vault access grant, and the
  settings/data model governing them, across the desktop client and Hub.
