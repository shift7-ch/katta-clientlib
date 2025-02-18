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
