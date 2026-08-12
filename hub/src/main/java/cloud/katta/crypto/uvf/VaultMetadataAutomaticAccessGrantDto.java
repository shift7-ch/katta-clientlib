/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */


package cloud.katta.crypto.uvf;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;


/**
 * Represents the upstream extension of <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>
 * for <a href="https://github.com/cryptomator/hub/pull/281">Web of Trust</a> of <a href="https://github.com/cryptomator/hub/">Cryptomator Hub</a> (upstream).
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>VaultMetadataJWEAutomaticAccessGrantDto</code></a>.
 */

@JsonPropertyOrder({
        VaultMetadataAutomaticAccessGrantDto.JSON_PROPERTY_ENABLED,
        VaultMetadataAutomaticAccessGrantDto.JSON_PROPERTY_TRUST_THRESHOLD
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class VaultMetadataAutomaticAccessGrantDto {

    public static final String JSON_PROPERTY_ENABLED = "enabled";
    private Boolean enabled;

    public static final String JSON_PROPERTY_TRUST_THRESHOLD = "trustThreshold";
    private Integer trustThreshold;

    public VaultMetadataAutomaticAccessGrantDto() {
    }

    public VaultMetadataAutomaticAccessGrantDto enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty(JSON_PROPERTY_ENABLED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Boolean getEnabled() {
        return enabled;
    }

    @JsonProperty(JSON_PROPERTY_ENABLED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public VaultMetadataAutomaticAccessGrantDto trustThreshold(Integer trustThreshold) {
        this.trustThreshold = trustThreshold;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty(JSON_PROPERTY_TRUST_THRESHOLD)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Integer getTrustThreshold() {
        return trustThreshold;
    }


    @JsonProperty(JSON_PROPERTY_TRUST_THRESHOLD)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTrustThreshold(Integer trustThreshold) {
        this.trustThreshold = trustThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        VaultMetadataAutomaticAccessGrantDto vaultMetadataJWEAutomaticAccessGrantDto = (VaultMetadataAutomaticAccessGrantDto) o;
        return Objects.equals(this.enabled, vaultMetadataJWEAutomaticAccessGrantDto.enabled) &&
                Objects.equals(this.trustThreshold, vaultMetadataJWEAutomaticAccessGrantDto.trustThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, trustThreshold);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VaultMetadataAutomaticAccessGrantDto{");
        sb.append("enabled=").append(enabled);
        sb.append(", trustThreshold=").append(trustThreshold);
        sb.append('}');
        return sb.toString();
    }
}

