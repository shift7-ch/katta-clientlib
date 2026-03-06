# Katta Admin CLI

## AWS Static Setup

````mermaid
flowchart TD
    A[New Katta Server] --> B("Hub Deployment: CSP settings Hub (&dagger;)")
    B --> C(Katta CLI: StorageProfileAwsStaticSetup)
    C --> D{Create Vault?}
    D --> E[AWS CLI/AdminConsole: Create Bucket]
    E --> F(Katta Desktop/Web Client: Create Vault)
    F --> D
````

(&dagger;) For standard AWS URLs, this is already part of the default setup.

## AWS STS Setup

````mermaid
flowchart TD
    A[New Katta Server] --> B(Katta CLI: AwsStsSetup)
    B --> C(Katta CLI: StorageProfileAwsStsSetup)
    C --> D{Create Vault?}
    D --> E[AWS CLI/AdminConsole: Create Bucket]
    E --> F("Katta Web Client: Create Vault (&Dagger;)")
    F --> D
````

(&Dagger;) Katta Desktop Client does not

#### Update thumbprints of TLS certificates

Thumbprints from the TLS certificates of the Keycloak endpoint need to be in place at AWS in the IAM identity provider endpoint verification and updated when
TLS certificates are renewed. Use `katta setup aws sts` to update thumbprints of renewed certificates.

> AWS secures communication with OIDC identity providers (IdPs) using our library of trusted Certificate Authorities (CAs). If your IdP relies on a certificate
> that isn't signed by one of these trusted CAs, then we secure communication using the thumbprints you specify.

