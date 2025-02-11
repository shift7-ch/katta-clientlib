/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;


import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.cryptomator.ContentWriter;
import ch.cyberduck.core.cryptomator.CryptoVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;

/**
 * Cryptomator vault implementation for Cipherduck (without masterkey file).
 */
public class HubCryptoVault extends CryptoVault {
    private static final Logger log = LogManager.getLogger(HubCryptoVault.class);

    // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
    //const jwtPayload: VaultConfigPayload = {
    //    jti: vaultId,
    //    format: 8,
    //    cipherCombo: 'SIV_GCM',
    //    shorteningThreshold: 220
    //};
    //const header = JSON.stringify({
    //    kid: kid,
    //    typ: 'jwt',
    //    alg: 'HS256',
    //    hub: hubConfig
    //});
    private static final VaultConfig VAULT_CONFIG = new VaultConfig(8, 220, CryptorProvider.Scheme.SIV_GCM, "HS256", null);

    public HubCryptoVault(final Path home) {
        super(home);
    }

    public HubCryptoVault(final Path home, final String masterkey, final String config, final byte[] pepper) {
        super(home);
    }

    /**
     * Upload vault template into existing bucket (permanent credentials)
     */
    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko check method signature?
    public synchronized Path create(final Session<?> session, final String region, final VaultCredentials credentials, final int version, final String metadata, final String rootDirHash) throws BackgroundException {
        final Path home = new Path(session.getHost().getDefaultPath(), EnumSet.of(AbstractPath.Type.directory));
        log.debug("Uploading vault template {} in {} ", home, session.getHost());

        // N.B. there seems to be no API to check write permissions without actually writing.
        if(!session.getFeature(ListService.class).list(home, new DisabledListProgressListener()).isEmpty()) {
            throw new BackgroundException("Bucket not empty", String.format("Cannot upload bucket %s in %s is not empty.", home, session.getHost()));
        }

        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
        //        zip.file('vault.cryptomator', this.vaultConfigToken);
        //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
        (new ContentWriter(session)).write(new Path(home, PreferencesFactory.get().getProperty("cryptomator.vault.config.filename"), EnumSet.of(AbstractPath.Type.file, AbstractPath.Type.vault)), metadata.getBytes(StandardCharsets.US_ASCII));
        Directory<?> directory = (Directory) session._getFeature(Directory.class);

        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 implement CryptoDirectory for uvf
        //        Path secondLevel = this.directoryProvider.toEncrypted(session, this.home.attributes().getDirectoryId(), this.home);
        final Path secondLevel = new Path(String.format("/%s/d/%s/%s/", session.getHost().getDefaultPath(), rootDirHash.substring(0, 2), rootDirHash.substring(2)), EnumSet.of(AbstractPath.Type.directory));
        final Path firstLevel = secondLevel.getParent();
        final Path dataDir = firstLevel.getParent();
        log.debug("Create vault root directory at {}", secondLevel);
        final TransferStatus status = (new TransferStatus()).withRegion(region);

        directory.mkdir(dataDir, status);
        directory.mkdir(firstLevel, status);
        directory.mkdir(secondLevel, status);
        return home;
    }

    @Override
    public HubCryptoVault load(final Session<?> session, final PasswordCallback prompt) throws BackgroundException {
        // no-interactive prompt in Cipherduck
        final String masterkey = prompt.prompt(session.getHost(), "", "", new LoginOptions()).getPassword();
        try {
            this.open(VAULT_CONFIG, new Masterkey(Base64.getDecoder().decode(masterkey)));
        }
        catch(IllegalArgumentException e) {
            throw new BackgroundException(e);
        }
        return this;
    }

    public Path getMasterkey() {
        // No master key in vault
        return null;
    }
}
