/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.cryptomator.impl.uvf.JWKSetUVFVaultMetadataProvider;
import ch.cyberduck.core.vault.VaultException;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

public class HubVaultMetadataUVFProvider extends JWKSetUVFVaultMetadataProvider {

    private static final String UVF_SPEC_VERSION_KEY_PARAM = "uvf.spec.version";

    /**
     *
     * @param apiURL  API URL that goes into custom header param "cloud.katta.origin"
     * @param vaultId The vault ID that goes into custom header param "cloud.katta.origin"
     * @param payload Vault metadata payload
     * @param keys    Member key and recovery key
     * @see <a href="https://github.com/encryption-alliance/unified-vault-format/tree/develop/vault%20metadata#jose-header">UVF Specification of JWE Header</a>
     */
    public HubVaultMetadataUVFProvider(final UVFMetadataPayload payload, final String apiURL, final UUID vaultId, final JWKSet keys) {
        this(new JWEObjectJSON(
                new JWEHeader.Builder(EncryptionMethod.A256GCM)
                        // kid goes into recipient-specific header
                        .customParam("cloud.katta.origin", URI.create(String.format("%s/vaults/%s/uvf/vault.uvf", apiURL, vaultId.toString())).normalize().toString())
                        .jwkURL(URI.create("jwks.json"))
                        .contentType("json")
                        .criticalParams(Collections.singleton(UVF_SPEC_VERSION_KEY_PARAM))
                        .customParam(UVF_SPEC_VERSION_KEY_PARAM, 1)
                        .build(), new Payload(
                new HashMap<String, Object>() {
                    {
                        put("fileFormat", payload.fileFormat());
                        put("nameFormat", payload.nameFormat());
                        put("seeds", payload.seeds());
                        put("initialSeed", payload.initialSeed());
                        put("latestSeed", payload.latestSeed());
                        put("kdf", payload.kdf());
                        put("kdfSalt", payload.kdfSalt());
                        put("org.cryptomator.automaticAccessGrant", payload.automaticAccessGrant());
                        put("cloud.katta.storage", payload.storage());
                    }
                }
        )), keys);
    }

    /**
     * Provider to create or load vault.
     *
     * @param keys          Member key and recovery key
     * @param vaultMetadata Encrypted metadata payload containing configuration and data related to the vault.
     */
    public HubVaultMetadataUVFProvider(final JWEObjectJSON vaultMetadata, final HubVaultKeys keys) {
        this(vaultMetadata, keys.memberKey());
    }

    public HubVaultMetadataUVFProvider(final JWEObjectJSON vaultMetadata, final JWK key) {
        super(vaultMetadata, new JWKSet(key));
    }

    public HubVaultMetadataUVFProvider(final JWEObjectJSON vaultMetadata, final JWKSet keys) {
        super(vaultMetadata, keys);
    }

    public UVFMetadataPayload getPayload() throws SecurityFailure {
        try {
            return UVFMetadataPayload.fromJSON(this.decrypt(), UVFMetadataPayload.class);
        }
        catch(JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
        catch(VaultException e) {
            throw new SecurityFailure(e.getDetail(false), e);
        }
    }
}
