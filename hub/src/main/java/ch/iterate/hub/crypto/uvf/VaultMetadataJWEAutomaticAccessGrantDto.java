/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */


package ch.iterate.hub.crypto.uvf;

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
        VaultMetadataJWEAutomaticAccessGrantDto.JSON_PROPERTY_ENABLED,
        VaultMetadataJWEAutomaticAccessGrantDto.JSON_PROPERTY_MAX_WOT_DEPTH
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class VaultMetadataJWEAutomaticAccessGrantDto {
    public static final String JSON_PROPERTY_ENABLED = "enabled";
    private Boolean enabled;

    public static final String JSON_PROPERTY_MAX_WOT_DEPTH = "maxWotDepth";
    private Integer maxWotDepth;

    public VaultMetadataJWEAutomaticAccessGrantDto() {
    }

    public VaultMetadataJWEAutomaticAccessGrantDto enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Get enabled
     *
     * @return enabled
     **/
    @javax.annotation.Nullable
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


    public VaultMetadataJWEAutomaticAccessGrantDto maxWotDepth(Integer maxWotDepth) {
        this.maxWotDepth = maxWotDepth;
        return this;
    }

    /**
     * Get maxWotDepth
     *
     * @return maxWotDepth
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")
    @JsonProperty(JSON_PROPERTY_MAX_WOT_DEPTH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public Integer getMaxWotDepth() {
        return maxWotDepth;
    }


    @JsonProperty(JSON_PROPERTY_MAX_WOT_DEPTH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setMaxWotDepth(Integer maxWotDepth) {
        this.maxWotDepth = maxWotDepth;
    }


    /**
     * Return true if this AutomaticAccessGrant object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        VaultMetadataJWEAutomaticAccessGrantDto vaultMetadataJWEAutomaticAccessGrantDto = (VaultMetadataJWEAutomaticAccessGrantDto) o;
        return Objects.equals(this.enabled, vaultMetadataJWEAutomaticAccessGrantDto.enabled) &&
                Objects.equals(this.maxWotDepth, vaultMetadataJWEAutomaticAccessGrantDto.maxWotDepth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxWotDepth);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AutomaticAccessGrant {\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    maxWotDepth: ").append(toIndentedString(maxWotDepth)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if(o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}

