/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.cryptomator.impl.uvf.VaultMetadataUVFProvider;
import ch.cyberduck.core.vault.VaultMetadataProvider;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

public class VaultIdMetadataUVFProvider implements VaultMetadataUVFProvider {

    private final UUID vaultId;
    private final UvfMetadataPayload.UniversalVaultFormatJWKS jwks;
    private final byte[] vaultMetadata;
    private final byte[] rootDirectoryMetadata;
    private final String hashedRootDirId;

    public VaultIdMetadataUVFProvider(final Host storage, final UUID vaultId,
                                      final UvfMetadataPayload.UniversalVaultFormatJWKS jwks, final UvfMetadataPayload vaultMetadata) throws JOSEException, JsonProcessingException {
        this(vaultId, jwks, vaultMetadata.encrypt(
                        String.format("%s/api", new HostUrlProvider(false, true).get(storage)),
                        vaultId,
                        jwks.toJWKSet()
                ).getBytes(StandardCharsets.US_ASCII),
                vaultMetadata.computeRootDirUvf(),
                vaultMetadata.computeRootDirIdHash()
        );
    }

    public VaultIdMetadataUVFProvider(final UUID vaultId, final UvfMetadataPayload.UniversalVaultFormatJWKS jwks, final byte[] vaultMetadata, final byte[] rootDirectoryMetadata, final String hashedRootDirId) {
        this.vaultId = vaultId;
        this.jwks = jwks;
        this.vaultMetadata = vaultMetadata;
        this.rootDirectoryMetadata = rootDirectoryMetadata;
        this.hashedRootDirId = hashedRootDirId;
    }

    public static VaultIdMetadataUVFProvider cast(VaultMetadataProvider provider) {
        if(provider instanceof VaultIdMetadataUVFProvider) {
            return (VaultIdMetadataUVFProvider) provider;
        }
        else {
            throw new IllegalArgumentException("Unsupported metadata type " + provider.getClass());
        }
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public UvfMetadataPayload.UniversalVaultFormatJWKS getJwks() {
        return jwks;
    }

    @Override
    public byte[] getMetadata() {
        return vaultMetadata;
    }

    @Override
    public byte[] getRootDirectoryMetadata() {
        return rootDirectoryMetadata;
    }

    @Override
    public String getDirPath() {
        return hashedRootDirId;
    }
}
