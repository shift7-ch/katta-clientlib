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

#### Update certs

Thumbprints of the TLS certificates need to be in place at AWS. Use Katta CLI's `AwsStsSetup` to update certs.


