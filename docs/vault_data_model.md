# Vault Data Model

Schema reference for `katta-clientlib` (hub module). Traces foreign-key relationships across four domains that are modeled separately in the codebase but share
IDs: **storage profiles**, the **/vault API**, the **vault.uvf JWE metadata**, and the **Hub bookmark** (a runtime Cyberduck construct, not a persisted entity).

> [!CAUTION]
> AI-generated content, manually skimmed.

## Scope

- **Storage profiles** (`StorageProfileDto`) — admin-configured S3 backends (static credentials or STS), discriminated by `protocol`.
- **/vault API** — `VaultDto` and the users, devices, groups, and recovery entities around it, from `hub/src/main/resources/openapi.json`.
- **vault.uvf JWE metadata** — the encrypted payload (`UVFMetadataPayload`) living inside `VaultDto.uvfMetadataFile`, from `cloud.katta.crypto.uvf.*`.
- **Hub bookmark** — the Cyberduck `Host`/`Profile` pair assembled at connection time from the other three (`cloud.katta.protocols.hub.*`); not stored on the
  server.

## Legend

```mermaid
classDiagram
    direction LR
    Supertype <|-- Subtype : discriminated subtype, e.g. StorageProfileS3StaticDto is-a StorageProfileDto
    Owner *-- Owned : embedded field, owned, e.g. UserDto embeds its devices
    Consumer --> Provider : fetched/associated via the API, not a literal embedded field
    Referrer ..> Referenced : foreign key, an id field referencing another entity
```

Color groups in the diagram: storage profile, /vault API entity, vault.uvf JWE metadata, Hub bookmark (runtime) — see the `classDef` fill colors on the diagram below.

## Diagram

Classes are grouped into two namespaces by where they're defined: `katta_server` for every schema declared in `hub/src/main/resources/openapi.json`, and
`client` for types that exist only in this client library — decrypted JWE payloads, key material, and the runtime Cyberduck bookmark. Fill color still marks the
four conceptual domains from the Legend above.

```mermaid
classDiagram
    direction LR

    namespace katta_server {
        class StorageProfileDto {
            +UUID id
            +String name
            +Protocol protocol
            +boolean archived
        }
        class StorageProfileS3StaticDto {
            +String endpoint
            +String bucketPrefix
            +String region
            +String[] regions
            +S3StorageClass storageClass
        }
        class StorageProfileS3STSDto {
            +String stsEndpoint
            +String stsRoleAccessBucketAssumeRoleWithWebIdentity
            +int stsDurationSeconds
            +String stsSessionTag
        }
        class VaultDto {
            +UUID id
            +String name
            +Instant creationTime
            +String description
            +boolean archived
            +int requiredEmergencyKeyShares
            +Map~String,String~ emergencyKeyShares
            +String uvfMetadataFile
            +String uvfKeySet
        }
        class MemberDto {
            +String id
            +Type type
            +String name
            +Role vaultRole
            +String ecdhPublicKey
            +String ecdsaPublicKey
        }
        class UserDto {
            +String id
            +String name
            +String email
            +boolean enabled
            +DeviceDto[] devices
            +String ecdhPublicKey
            +String ecdsaPublicKey
        }
        class GroupDto {
            +String id
            +String name
            +int memberSize
            +int vaultCount
        }
        class DeviceDto {
            +String id
            +String name
            +Type1 type
            +String publicKey
            +String userPrivateKey
            +String owner
        }
        class RecoveryProcessDto {
            +UUID id
            +UUID vaultId
            +Type2 type
            +int requiredKeyShares
            +Map~String,RecoveredKeyShareDto~ recoveredKeyShares
        }
        class RecoveredKeyShareDto {
            +String processPrivateKey
            +String unrecoveredKeyShare
            +String recoveredKeyShare
        }
        class CreateS3STSBucketDto {
            +String vaultId
            +UUID storageConfigId
            +String vaultUvf
            +String dirUvf
            +String awsAccessKey
            +String sessionToken
        }
    }

    namespace client {
        class UVFMetadataPayload {
            +String fileFormat
            +String nameFormat
            +Map~String,String~ seeds
            +String initialSeed
            +String latestSeed
            +String kdf
            +String kdfSalt
        }
        class VaultMetadataStorageDto {
            +String provider
            +String bucket
            +String nickname
            +String region
            +String username
            +String password
        }
        class VaultMetadataAutomaticAccessGrantDto {
            +boolean enabled
            +int trustThreshold
        }
        class UVFAccessTokenPayload {
            +String memberKey
            +String recoveryKey
        }
        class HubVaultKeys {
            +OctetSequenceKey memberKey
            +P384KeyPair recoveryKey
        }
        class HubProtocol {
            +String identifier = hub
            +String scheme = https
        }
        class Host {
            +String hostname
            +Credentials credentials
        }
        class HubStorageProfile {
            +String bucket
        }
    }

    StorageProfileDto <|-- StorageProfileS3StaticDto
    StorageProfileDto <|-- StorageProfileS3STSDto
    VaultDto "1" --> "0..*" MemberDto: members (GET .../members)
    MemberDto ..> UserDto: id=id, type=USER (FK)
    MemberDto ..> GroupDto: id=id, type=GROUP (FK)
    UserDto "1" *-- "0..*" DeviceDto: devices
    DeviceDto ..> UserDto: owner=id (FK)
    RecoveryProcessDto ..> VaultDto: vaultId=id (FK)
    RecoveryProcessDto "1" *-- "1..*" RecoveredKeyShareDto: recoveredKeyShares
    RecoveredKeyShareDto ..> UserDto: recoveredKeyShares map key=id (FK)
    CreateS3STSBucketDto ..> VaultDto: vaultId=id (FK)
    CreateS3STSBucketDto ..> StorageProfileDto: storageConfigId=id (FK)
    VaultDto ..> UVFMetadataPayload: uvfMetadataFile decrypts to this payload
    UVFMetadataPayload *-- VaultMetadataStorageDto: storage
    UVFMetadataPayload *-- VaultMetadataAutomaticAccessGrantDto: automaticAccessGrant
    VaultMetadataStorageDto ..> StorageProfileDto: provider=id (FK)
    VaultDto ..> HubVaultKeys: uvfKeySet is a JWKS of these keys
    HubVaultKeys ..> UVFAccessTokenPayload: reconstructed from memberKey/recoveryKey fields
    UVFAccessTokenPayload ..> UserDto: recipient=ecdhPublicKey (FK)
    Host --> HubStorageProfile: profile
    Host ..> HubProtocol: protocol
    HubStorageProfile ..> StorageProfileDto: (ctor arg)=id (FK)
    Host ..> VaultDto: volume path=id (FK)

classDef storageGrp fill:#e0f2f1
classDef apiGrp fill:#e3f2fd
classDef jweGrp fill:#fff3e0
classDef bookmarkGrp fill:#e8f5e9

class StorageProfileDto storageGrp
class StorageProfileS3StaticDto storageGrp
class StorageProfileS3STSDto storageGrp
class VaultDto apiGrp
class MemberDto apiGrp
class UserDto apiGrp
class GroupDto apiGrp
class DeviceDto apiGrp
class RecoveryProcessDto apiGrp
class RecoveredKeyShareDto apiGrp
class CreateS3STSBucketDto apiGrp
class UVFMetadataPayload jweGrp
class VaultMetadataStorageDto jweGrp
class VaultMetadataAutomaticAccessGrantDto jweGrp
class UVFAccessTokenPayload jweGrp
class HubVaultKeys jweGrp
class HubProtocol bookmarkGrp
class Host bookmarkGrp
class HubStorageProfile bookmarkGrp
```

## Foreign keys, at a glance

| Source                                        | Field             | Target                        | Note                                                                                                       |
|-----------------------------------------------|-------------------|-------------------------------|------------------------------------------------------------------------------------------------------------|
| `VaultMetadataStorageDto`                     | `provider`        | `StorageProfileDto.id`        | Set at vault creation from the chosen profile; only visible after decrypting `vault.uvf`.                  |
| `CreateS3STSBucketDto`                        | `storageConfigId` | `StorageProfileDto.id`        | Same FK, plaintext this time — sent to `PUT /api/storage/{vaultId}` when provisioning the bucket.          |
| `HubStorageProfile`                           | (constructor arg) | `StorageProfileDto.id`        | The bookmark's connection profile is built directly from a `StorageProfileDtoWrapper`.                     |
| `CreateS3STSBucketDto` / `RecoveryProcessDto` | `vaultId`         | `VaultDto.id`                 | Also the path parameter on nearly every `/api/vaults/{vaultId}/...` route.                                 |
| `Host`                                        | volume path       | `VaultDto.id`                 | The bucket object path is `bucketPrefix + vaultId`; the bookmark's root volume encodes it.                 |
| `MemberDto` / `AuthorityDto`                  | `id` (+ `type`)   | `UserDto.id` or `GroupDto.id` | Polymorphic FK — `type` discriminates which table the id belongs to.                                       |
| `DeviceDto`                                   | `owner`           | `UserDto.id`                  | Also the path param in the deprecated `GET /vaults/{vaultId}/keys/{deviceId}`.                             |
| `UVFAccessTokenPayload`                       | JWE recipient     | `UserDto.ecdhPublicKey`       | Delivered per-user via `/vaults/{vaultId}/access-token(s)`, encrypted so only that key holder can open it. |
| `RecoveryProcessDto`                          | `vaultId`         | `VaultDto.id`                 | Emergency-access process; `recoveredKeyShares` map keys are authority ids (FK to User/Group).              |

## Simplifications

- **Hub bookmark isn't a server entity.** `Host` + `HubStorageProfile` is a Cyberduck runtime construct assembled in `HubUVFVaultProvider.create()`/`.load()` —
  nothing here is persisted on the Hub server.
- **`AuthorityDto`** (`oneOf` `UserDto` \| `GroupDto` \| `MemberDto`, discriminated by `type`) is collapsed into direct edges to `UserDto`/`GroupDto` for
  legibility.
- Legacy pre-UVF fields on `VaultDto` (`masterkey`, `iterations`, `salt`, `authPublicKey`, `authPrivateKey`) and the read-only list projection
  `VaultDtoWithRole` are omitted — they don't carry new foreign keys.
- Field lists are trimmed to identifying and FK-bearing attributes; see `hub/src/main/resources/openapi.json` for the full OpenAPI schemas.

---
Sources: `hub/src/main/resources/openapi.json` · `cloud.katta.crypto.uvf.*` · `cloud.katta.protocols.hub.*`
