# Katta: the secure and easy way to work in teams

Katta brings zero-config storage management and zero-knowledge key management for teams and organizations.

## Katta Admin CLI

This CLI program is used to configure a Katta Server including its S3 storage backend. Supported storage backend configurations are:

- AWS S3 accessed using static access keys
- AWS S3 accessed using AWS Security Token Service (STS) issuing temporary access keys from OIDC access token obtained by user from Keycloak identity provider.
- Generic S3-compatible provider accessed using static access credentials.
- MinIO accessed using Security Token Service (STS) with OIDC.

### Setup AWS using OIDC Provider and Security Token Service (STS) with `setup` command

Set up AWS as storage backend for Katta Server. Configures identity provider and roles in IAM to restrict access to S3 buckets to users authenticated by
Keycloak.

```bash
katta setup aws \
  --realmUrl <realm-url>
```

**Required Options:**

- `--realmUrl`: Keycloak realm URL with scheme. Example: `https://keycloak.default.domain/realms/cryptomator`

**Additional Options:**

- `--profileName`: AWS profile to load AWS credentials from (see `~/.aws/credentials`)
- `--clientId`: Client Ids for the OIDC provider
- `--roleNamePrefix`: Prefix used for IAM role names. Defaults to `katta-`.
- `--bucketPrefix`: Prefix used when creating buckets for this storage profile. Defaults to `katta-`.

### Configure storage profile in AWS Setup using `storageprofile` command

Uploads a storage profile to Katta Server for use with AWS S3.
Requires [Setup AWS using OIDC Provider and Security Token Service (STS)](#setup-aws-using-oidc-provider-and-security-token-service-sts-with-setup-command).

```bash
katta storageprofile aws sts \
  --hubUrl <hub-url> \
  --awsAccountId <aws-account-id> \
  --region <aws-region>
```

**Required Options:**

- `--hubUrl`: Hub URL. Example: `https://hub.default.katta.cloud/`. Keycloak auth and token endpoints are fetched automatically from `<hub-url>/api/config`.
- `--awsAccountId`: AWS Account ID. A 12-digit number, such as 012345678901, that uniquely identifies an AWS account.
- `--region`: Bucket region. Example: `eu-west-1`

**Additional Options:**

- `--roleNamePrefix`: Prefix used for IAM role names. Defaults to `katta-`.
- `--bucketPrefix`: Prefix used when creating buckets for this storage profile. Defaults to `katta-`.
- `--authUrl`: Keycloak auth endpoint URL. Overrides the value fetched from `--hubUrl`.
- `--tokenUrl`: Keycloak token endpoint URL. Overrides the value fetched from `--hubUrl`.

### Configure storage profile for a generic S3-compatible provider using `storageprofile` command

Uploads a storage profile to Katta Server for use with any S3-compatible storage provider using static access credentials.
Unlike STS-based profiles, no temporary credentials are issued; the server uses static access key credentials directly.

```bash
katta storageprofile s3 static \
  --hubUrl <hub-url> \
  --endpointUrl <s3-endpoint-url> \
  --region <region>
```

**Required Options:**

- `--hubUrl`: Hub URL. Example: `https://hub.default.katta.cloud/`
- `--endpointUrl`: S3 endpoint URL. Example: `https://s3.example.com` or `https://s3.example.com:9000`
- `--region`: Default bucket region. Example: `us-east-1`

**Additional Options:**

- `--bucketPrefix`: Prefix used when creating buckets for this storage profile. Defaults to `katta-`.
- `--regions`: Additional bucket regions. Example: `--regions us-east-1 --regions us-west-2`
- `--name`: Display name for the storage profile.
- `--uuid`: UUID for the storage profile (auto-generated if omitted).

### Configure storage profile for MinIO using `storageprofile` command

Uploads a storage profile to Katta Server for use with MinIO STS. Requires MinIO STS setup with an OIDC provider.

Unlike AWS, MinIO does not support role chaining, so the same role ARN is used for both bucket creation and hub access.
MinIO uses the `${jwt:client_id}` policy variable to scope bucket access per vault.

See also: [MinIO setup documentation](https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#minio).

```bash
katta storageprofile minio sts \
  --hubUrl <hub-url> \
  --endpointUrl <minio-endpoint-url> \
  --region <region> \
  --stsRoleCreateBucketClient <role-arn> \
  --stsRoleCreateBucketHub <role-arn> \
  --stsRoleAccessBucket <role-arn>
```

**Required Options:**

- `--hubUrl`: Hub URL. Example: `https://hub.default.katta.cloud/`
- `--endpointUrl`: MinIO endpoint URL (S3 API). Example: `https://minio.example.com` or `https://minio.example.com:9000`
- `--region`: Default bucket region. Example: `us-east-1`
- `--stsRoleCreateBucketClient`: MinIO role ARN for bucket creation by the Cryptomator client (from `mc idp openid ls` for the `cryptomator` client).
- `--stsRoleCreateBucketHub`: MinIO role ARN for bucket creation by Cryptomator Hub (from `mc idp openid ls` for the `cryptomatorhub` client).
- `--stsRoleAccessBucket`: MinIO role ARN for bucket access (from `mc idp openid ls` for the `cryptomatorvaults` client).

**Additional Options:**

- `--bucketPrefix`: Prefix used when creating buckets for this storage profile. Defaults to `katta-`.
- `--regions`: Additional bucket regions. Example: `--regions us-east-1 --regions us-west-2`
- `--name`: Display name for the storage profile.
- `--uuid`: UUID for the storage profile (auto-generated if omitted).

### Generate shell completion script with `completion` command

Generate a bash completion script for the `katta` CLI and install it for the current shell session.

```bash
source <(katta completion)
```

To persist completion across sessions, write the script to a file and source it from your shell profile:

```bash
katta completion > ~/.bash_completion.d/katta
echo 'source ~/.bash_completion.d/katta' >> ~/.bashrc
```

**Options:**

- `--shell`: Shell to generate completion for. Only `bash` is supported. Defaults to `bash`.

#### Update thumbprints of TLS certificates

Thumbprints from the TLS certificates of the Keycloak endpoint need to be in place at AWS in the IAM identity provider endpoint verification and updated when
TLS certificates are renewed. Use `katta setup aws` to update thumbprints of renewed certificates.

> AWS secures communication with OIDC identity providers (IdPs) using our library of trusted Certificate Authorities (CAs). If your IdP relies on a certificate
> that isn't signed by one of these trusted CAs, then we secure communication using the thumbprints you specify.

