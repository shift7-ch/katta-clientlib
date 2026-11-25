/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import java.util.Objects;

import cloud.katta.crypto.JWEPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Custom fields in the payload of <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault-metadata/README.md"><code>vault.uvf</code> metadata</a>.
 * Seeds, key derivation and formats of the payload are handled by cryptolib.
 * <ul>
 *  <li>org.cryptomator.automaticAccessGrant (upstream)</li>
 *  <li>cloud.katta.storage</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UVFMetadataPayload extends JWEPayload {

    @JsonProperty(value = "org.cryptomator.automaticAccessGrant", required = true)
    VaultMetadataAutomaticAccessGrantDto automaticAccessGrant;

    @JsonProperty(value = "cloud.katta.storage", required = true)
    VaultMetadataStorageDto storage;

    public UVFMetadataPayload() {
    }

    public VaultMetadataAutomaticAccessGrantDto automaticAccessGrant() {
        return automaticAccessGrant;
    }

    public UVFMetadataPayload withAutomaticAccessGrant(final VaultMetadataAutomaticAccessGrantDto vaultMetadataJWEAutomaticAccessGrantDto) {
        this.automaticAccessGrant = vaultMetadataJWEAutomaticAccessGrantDto;
        return this;
    }

    public VaultMetadataStorageDto storage() {
        return storage;
    }

    public UVFMetadataPayload withStorage(final VaultMetadataStorageDto backend) {
        this.storage = backend;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final UVFMetadataPayload that = (UVFMetadataPayload) o;
        return Objects.equals(automaticAccessGrant, that.automaticAccessGrant) && Objects.equals(storage, that.storage);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(automaticAccessGrant);
        result = 31 * result + Objects.hashCode(storage);
        return result;
    }
}
