# Katta: the secure and easy way to work in teams

Katta brings zero-config storage management and zero-knowledge key management for teams and organizations.

## Katta Admin CLI

This CLI program is used to configure a Katta Server including its S3 storage backend. Supported storage backend configurations are:

- AWS S3 accessed using static access keys
- AWS S3 accessed using AWS Security Token Service (STS) issuing temporary access keys from OIDC access token obtained by user from Keycloak identity provider.

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

### Configure storage profile in AWS Setup using `storageprofile` command

Uploads a storage profile to Katta Server for use with AWS S3.
Requires [Setup AWS using OIDC Provider and Security Token Service (STS)](#setup-aws-using-oidc-provider-and-security-token-service-sts-with-setup-command).

```bash
katta storageprofile aws sts \
  --hubUrl <hub-url> \
  --awsAccountId <aws-account-id> \
  --region <aws-region> \
  --authUrl <auth-url> \
  --tokenUrl <token-url> \
```

**Required Options:**

- `--hubUrl`: Hub URL
- `--awsAccountId`: AWS Account ID. A 12-digit number, such as 012345678901, that uniquely identifies an AWS account.
- `--region`: Bucket region. Example: `eu-west-1`
- `--authUrl`: Keycloak URL. Example: `https://keycloak.default.katta.cloud/kc/realms/cryptomator/protocol/openid-connect/auth`
- `--tokenUrl`: Keycloak URL. Example: `https://keycloak.default.katta.cloud/kc/realms/cryptomator/protocol/openid-connect/token`

#### Update thumbprints of TLS certificates

Thumbprints from the TLS certificates of the Keycloak endpoint need to be in place at AWS in the IAM identity provider endpoint verification and updated when
TLS certificates are renewed. Use `katta setup aws` to update thumbprints of renewed certificates.

> AWS secures communication with OIDC identity providers (IdPs) using our library of trusted Certificate Authorities (CAs). If your IdP relies on a certificate
> that isn't signed by one of these trusted CAs, then we secure communication using the thumbprints you specify.

