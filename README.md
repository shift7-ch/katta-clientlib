[![CI Build](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml/badge.svg)](https://github.com/shift7-ch/katta-clientlib/actions/workflows/build.yml)

# Katta: the secure and easy way to work in teams

Katta bring zero-config storage management and zero-knowledge key management for teams and organizations.

## Katta Client Library

This library implements the [Katta Server API](https://github.com/shift7-ch/katta-docs/blob/main/OVERVIEW.md)
as [Cyberduck](https://github.com/iterate-ch/cyberduck) protocol features
for the Katta Client.

Features:

* Client code is generated for Katta Backend API through `openapi.json`
* Katta Server interaction (workflows like first login, vault creation, (automatic) access grant and sync of storage profiles and vaults): implementation and
  integration/regression tests.
* `S3` and `S3STS` Cyberduck protocols for Katta (see [Katta S3 Modes](https://github.com/shift7-ch/katta-docs/blob/main/OVERVIEW.md#katta-s3-modes)) incl.
  token exchange and AWS role chaining. Cyberduck handles OAuth 2.0 token management (authorization code grant and token refresh).

## Flow to retrieve user keys

```mermaid
sequenceDiagram
    actor user as User
    activate user
    participant session as Session
    activate session
    user->>session: Open Connection
    participant katta as Katta API Server
    activate katta
    session->>katta: Retrieve user information
    participant keychain as Password Store
    session->>+keychain: Retrieve device keys
    keychain->>-session: Previously saved device key
    alt Use saved device key
        user->>katta: Retrieve device specific user keys
        opt: 404 Not found
            Note over user,katta: Device key not found on server
            session->>user: Prompt for account key
            user->>session: Input account key
            session->>session: Recover user keys
            session->>katta: Upload device specific user keys
        end
        katta->>session: Return device specific user keys
        session->>session: Decrypt with device key
    else Device key not available
        alt Recover user keys
            Note over user,katta: Setting up new device
            session->>user: Prompt for account key
            user->>session: Input account key
            session->>session: Recover user keys
        else No user keys stored on Katta Server
            Note over user,katta: Setting up new user keys and account key
            session->>user: Generate account key and prompt for device name
            user->>session: Input device name
            session->>session: Generate user key pair
            session->>katta: Upload user keys with account key
            session->>session: Generate new device key
        end
        session->>katta: Upload device specific user keys
        session->>keychain: Save device keys
    end
    session->>user: Return user keys
    deactivate katta
    deactivate session
    deactivate user
```

## Flow to authenticate and access vaults

```mermaid
sequenceDiagram
    actor User

    participant session as Session
    participant katta as Katta API Server

    Note right of session: client_id=cryptomator

    activate session
    User->>session: Open Connection
    activate katta
    session->>katta: GET /api/config
    Note over session,katta: Retrieve Public Discovery Configuration
    katta->>session: application/json

    participant keycloak as Keycloak Server
    activate keycloak
    session->>+keycloak: POST /realms/cryptomator/protocol/openid-connect/token
    Note over session,keycloak: OpenID Connect Token Exchange
    keycloak->>-session: OIDC Tokens

    participant keychain as Password Store
    activate keychain
    session->>keychain: Save OIDC Tokens

    session->>katta: GET /api/users/me?withDevices=true
    Note over session,katta: Retrieve public keys and registered devices
    katta->>session: application/json

    session->>katta: PUT /api/users/me
    Note over session,katta: Upload User Keys
    katta->>session: 201 Created application/json

    session->>katta: PUT /api/devices/8ED7FAA95D4F912FFC80585D776261C8D32205FB03B59BF0311193DD5E482D90
    Note over session,katta: Register Device
    katta->>session: 201 Created application/json

    loop Storage Profile Sync
        session->>katta: GET /api/storageprofile
        Note over session,katta: Retrieve storage configurations
        katta->>session: application/json
    end
    loop Storage Vault Sync
        session->>katta: GET /api/vaults/accessible
        katta->>session: application/json
    end
    deactivate katta

    participant vault as S3AutoLoadVaultSession
    activate vault
    vault->>keychain: Lookup OIDC tokens
    keychain->>vault: Return OIDC tokens
    deactivate keychain
    activate keycloak

    opt: Expired OIDC Tokens
        vault->>+katta: Refresh OIDC Tokens
        katta->>-vault: OIDC Tokens
    end

    opt: Exchange OIDC token to scoped token using OAuth 2.0 Token Exchange
        vault->>keycloak: Exchange OIDC Access Token
        keycloak->>vault: Return Scoped Access Token
    end
    deactivate keycloak

    opt: AssumeRoleWithWebIdentity
        participant sts as STS API Server
        vault->>+sts: Retrieve Temporary Tokens
        Note over vault,sts: Assume role with OIDC Id token
        sts->>-vault: STS Tokens
        opt: AssumeRole
            vault->>+sts: Retrieve Temporary Tokens
            Note over vault,sts: Assume role with previously obtained temporary access token
            sts->>-vault: STS Tokens
        end
    end

    participant s3 as S3 API Server

    vault->>+s3: GET /bucket
    Note over vault,s3: Access vault with AWS4-HMAC-SHA256 authorization
    s3->>-vault: ListBucketResult

    vault->>+katta: GET /api/vaults/c62d1ffe-7bab-4ec9-a36a-327f9b7b8f9e/access-token
    Note over vault,katta: Retrieve vault access token
    katta->>-vault: JWE
    vault->>+katta: GET /api/vaults/c62d1ffe-7bab-4ec9-a36a-327f9b7b8f9e
    Note over vault,katta: Retrieve vault UVF metadata
    katta->>-vault: UVF Payload
    vault->>vault: Unlock Vault

    vault->>+User: Display Vault
    deactivate vault
    deactivate session
```
