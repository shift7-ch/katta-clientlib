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

Integration tests start the Docker Compose environment of [katta-compose](https://github.com/shift7-ch/katta-compose)
included with its Git URL in [`compose.yaml`](hub/src/test/resources/compose.yaml). Docker Compose fetches the
referenced commit on first use. To run integration tests with a local checkout of katta-compose instead, replace the
Git URL with the absolute path to `compose.yaml` in the checkout.

## Integration Test Environment

Integration tests run Katta Server, Keycloak, PostgreSQL and MinIO with [katta-compose](https://github.com/shift7-ch/katta-compose),
using the setup files and env files of this project in [`hub/src/test/resources`](hub/src/test/resources).
katta-compose renders the Keycloak realm from the Helm chart of Katta Server.
Refer to katta-compose for the One-Stop Shop Demo, its profiles and endpoints.

To start the environment of the integration tests yourself, use

```bash
export SETUP_DIR=$PWD/hub/src/test/resources/setup
docker compose -f hub/src/test/resources/compose.yaml --env-file hub/src/test/resources/.local.env --profile local up --wait
docker compose -f hub/src/test/resources/compose.yaml --env-file hub/src/test/resources/.local.env --profile local down
```

For the `hybrid` profile with Keycloak and MinIO on `testing.katta.cloud` and AWS S3, use
[`.chipotle.env`](hub/src/test/resources/.chipotle.env) instead. CI writes its values from a repository secret.

### Provisioned Users

#### Keycloak

The realm of katta-compose provisions the administrator `admin` with password `admin`. Before the tests of the `local`
profile, [`KattaTestRealm`](hub/src/test/java/cloud/katta/testsetup/KattaTestRealm.java) adds the test configuration:

- It enables direct access grants in client `cryptomator` for the password grant of the integration tests.
- It creates the user `HUB_USER` with password `HUB_PASSWORD` of the env file (`alice` with password `asd`) and the roles
  `user` and `create-vaults` using the API of Katta Server.

The setup is idempotent. When you start the environment yourself with the commands above, run the integration tests with
`HubTestSetupDockerExtension.LocalAlreadyRunning`, which adds the test configuration to the running environment.

#### MinIO
MinIO provisions the root user `minioadmin` with password `minioadmin`, and the user `testuser` with password `top-secret`
for static storage access.
