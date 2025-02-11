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
 * Represents the downstream extension of <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>
 * to describe the vault storage location (an S3 bucket) in <a href="https://github.com/shift7-ch/katta-server/">Katta Server</a> (downstream).
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>VaultMetadataJWEBackendDto</code></a>.
 */
@JsonPropertyOrder({
        VaultMetadataJWEBackendDto.JSON_PROPERTY_PROVIDER,
        VaultMetadataJWEBackendDto.JSON_PROPERTY_DEFAULT_PATH,
        VaultMetadataJWEBackendDto.JSON_PROPERTY_NICKNAME,
        VaultMetadataJWEBackendDto.JSON_PROPERTY_REGION,
        VaultMetadataJWEBackendDto.JSON_PROPERTY_USERNAME,
        VaultMetadataJWEBackendDto.JSON_PROPERTY_PASSWORD
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class VaultMetadataJWEBackendDto {
    public static final String JSON_PROPERTY_PROVIDER = "provider";
    private String provider;

    public static final String JSON_PROPERTY_DEFAULT_PATH = "defaultPath";
    private String defaultPath;

    public static final String JSON_PROPERTY_NICKNAME = "nickname";
    private String nickname;

    public static final String JSON_PROPERTY_REGION = "region";
    private String region;

    public static final String JSON_PROPERTY_USERNAME = "username";
    private String username;

    public static final String JSON_PROPERTY_PASSWORD = "password";
    private String password;

    public VaultMetadataJWEBackendDto() {
    }

    public VaultMetadataJWEBackendDto provider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Get provider
     *
     * @return provider
     **/
    @javax.annotation.Nonnull
    @ApiModelProperty(required = true, value = "")
    @JsonProperty(JSON_PROPERTY_PROVIDER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getProvider() {
        return provider;
    }


    @JsonProperty(JSON_PROPERTY_PROVIDER)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setProvider(String provider) {
        this.provider = provider;
    }


    public VaultMetadataJWEBackendDto defaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
        return this;
    }

    /**
     * Get defaultPath
     *
     * @return defaultPath
     **/
    @javax.annotation.Nonnull
    @ApiModelProperty(required = true, value = "")
    @JsonProperty(JSON_PROPERTY_DEFAULT_PATH)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getDefaultPath() {
        return defaultPath;
    }


    @JsonProperty(JSON_PROPERTY_DEFAULT_PATH)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }


    public VaultMetadataJWEBackendDto nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    /**
     * Get nickname
     *
     * @return nickname
     **/
    @javax.annotation.Nonnull
    @ApiModelProperty(required = true, value = "")
    @JsonProperty(JSON_PROPERTY_NICKNAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getNickname() {
        return nickname;
    }


    @JsonProperty(JSON_PROPERTY_NICKNAME)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public VaultMetadataJWEBackendDto region(String region) {
        this.region = region;
        return this;
    }

    /**
     * Get region
     *
     * @return region
     **/
    @javax.annotation.Nonnull
    @ApiModelProperty(required = true, value = "")
    @JsonProperty(JSON_PROPERTY_REGION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)

    public String getRegion() {
        return region;
    }


    @JsonProperty(JSON_PROPERTY_REGION)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setRegion(String region) {
        this.region = region;
    }


    public VaultMetadataJWEBackendDto username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Get username
     *
     * @return username
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")
    @JsonProperty(JSON_PROPERTY_USERNAME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getUsername() {
        return username;
    }


    @JsonProperty(JSON_PROPERTY_USERNAME)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setUsername(String username) {
        this.username = username;
    }


    public VaultMetadataJWEBackendDto password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Get password
     *
     * @return password
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")
    @JsonProperty(JSON_PROPERTY_PASSWORD)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getPassword() {
        return password;
    }


    @JsonProperty(JSON_PROPERTY_PASSWORD)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * Return true if this VaultJWEBackendDto object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        VaultMetadataJWEBackendDto vaultMetadataJWEBackendDto = (VaultMetadataJWEBackendDto) o;
        return Objects.equals(this.provider, vaultMetadataJWEBackendDto.provider) &&
                Objects.equals(this.defaultPath, vaultMetadataJWEBackendDto.defaultPath) &&
                Objects.equals(this.nickname, vaultMetadataJWEBackendDto.nickname) &&
                Objects.equals(this.region, vaultMetadataJWEBackendDto.region) &&
                Objects.equals(this.username, vaultMetadataJWEBackendDto.username) &&
                Objects.equals(this.password, vaultMetadataJWEBackendDto.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, defaultPath, nickname, region, username, password);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class VaultJWEBackendDto {\n");
        sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
        sb.append("    defaultPath: ").append(toIndentedString(defaultPath)).append("\n");
        sb.append("    nickname: ").append(toIndentedString(nickname)).append("\n");
        sb.append("    region: ").append(toIndentedString(region)).append("\n");
        sb.append("    username: ").append(toIndentedString(username)).append("\n");
        sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

