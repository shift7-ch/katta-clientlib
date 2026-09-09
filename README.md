[![CI Build](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml)
[![Integration Tests](https://github.com/shift7-ch/katta-clientlib/actions/workflows/integration.yml/badge.svg)](https://github.com/shift7-ch/katta-clientlib/actions/workflows/integration.yml)

# Katta: the secure and easy way to work in teams

Katta brings zero-config storage management and zero-knowledge key management for teams and organizations.

## Katta Client Library

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
| [`admin-cli`](admin-cli/README.md) | `katta-admin-cli`       | Standalone command-line tool (picocli, with an optional GraalVM native-image build) to configure a Katta Server and its S3 storage profiles.                                                                                                             |
| [`test`](test)                     | `katta-clientlib-tests` | Shared test fixtures and the Docker Compose environment (Katta Server, Keycloak, MinIO) packaged as a `test-jar` and reused by the integration tests of the other modules.                                                                               |

## Katta Admin CLI

Additionally, this repository contains the [Katta Admin CLI](admin-cli/README.md) used to configure a Katta Server including available S3 storage profiles.

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

## One-Stop Shop Demo with Docker Compose

> [!TIP]
> Open Katta Web at http://localhost:8280 in your web browser.

### Local Profile

> [!WARNING]
> You are required to configure `HUB_INITIAL_LICENSE` and `HUB_INITIAL_ID` in [.local.env](test/src/test/resources/.local.env)

Running full stack locally consisting of

- Katta Server
- Keycloak
- MinIO with Docker Compose.

```bash
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile local \\
--env-file test/src/test/resources/.local.env up --wait
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile local \\
--env-file test/src/test/resources/.local.env down
```

> [!TIP]
> Configure MinIO for STS Storage Access Mode. Refer to
the [Admin CLI Docmentation](admin-cli/README.md#setup-minio-using-oidc-provider-and-security-token-service-sts-with-setup-command).
> ```bash
> katta setup minio --hubUrl http://localhost:8280 --endpointUrl http://localhost:9100 --accessKey=minioadmin --secretKey=minioadmin
> ```

### Hybrid Test Environment Profile

For integration tests with

- Katta Server
- Keycloak, MinIO on `testing.katta.cloud` and AWS S3.

```bash
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile hybrid \\
--env-file test/src/test/resources/.chipotle.env up --wait
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile hybrid \\
--env-file test/src/test/resources/.chipotle.env down
```

#### Local Demo Profile

Running full stack locally, including the deployment of storage profiles for MinIO with static and STS Storage Access Mode:

```bash
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile demo \\
--env-file test/src/test/resources/.local.env up --wait
docker compose -f test/src/test/resources/docker-compose-hub-keycloak-minio.yml --profile demo \\
--env-file test/src/test/resources/.local.env down
```

> [!TIP]
> To access with Katta Desktop over plain HTTP (no HTTPS/TLS required),
copy [Katta Server.cyberduckprofile](test/src/test/resources/Katta%20Server.cyberduckprofile) to:

- **macOS** `~/Library/Group Containers/KD9X6Y7KA2.cloud.katta.desktop/Library/Application Support/Katta/Profiles`
- **Windows** `%APPDATA%\Katta\Profiles`

### Provisioned Users

The following users are automatically provisioned for testing:

| User         | Password     | Katta Roles (`realmRoles`) | Keycloak Roles (`realm-management`)                            | MinIO Roles       |
|--------------|--------------|----------------------------|----------------------------------------------------------------|-------------------|
| `admin`      | `admin`      | `admin`                    | `realm-admin`                                                  |                   |
| `alice`      | `asd`        | `user`, `create-vaults`    |                                                                |                   |
| `bob`        | `asd`        | `user`, `create-vaults`    |                                                                |                   |
| `carol`      | `asd`        | `user`                     |                                                                |                   |
| `carol`      | `asd`        | `user`                     |                                                                |                   |
| `erin`       | `asd`        | `user`                     |                                                                |                   |
| `syncer`     | `asd`        | `syncer`                   | `view-users`, `view-clients`, `manage-users`, `manage-clients` |                   |
| `minioadmin` | `minioadmin` |                            |                                                                | `MINIO_ROOT_USER` |

### Endpoints

The following endpoints are available for testing:

| Component     | URL                   | Discovery                                                                 |
|---------------|-----------------------|---------------------------------------------------------------------------|
| Katta Web     | http://localhost:8280 | http://localhost:8280/api/config                                          |
| Keycloak      | http://localhost:8380 | http://localhost:8380/realms/cryptomator/.well-known/openid-configuration |
| MinIO Console | http://localhost:9101 |                                                                           |
