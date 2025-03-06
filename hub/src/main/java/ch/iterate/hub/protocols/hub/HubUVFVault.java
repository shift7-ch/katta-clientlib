/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.cryptomator.ContentWriter;
import ch.cyberduck.core.cryptomator.UVFVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/**
 * Unified vault format (UVF) implementation for Katta
 */
public class HubUVFVault extends UVFVault {
    private static final Logger log = LogManager.getLogger(HubUVFVault.class);

    private final Session<?> storage;
    private final Path home;

    public HubUVFVault(final Session<?> storage, final Path home) {
        super(home);
        this.storage = storage;
        this.home = home;
    }

    public Path encrypt(final Session<?> session, final Path file, final byte[] directoryId, final boolean metadata) throws BackgroundException {
        log.debug("Use directory ID '{}' for folder {}", directoryId, file);
        return super.encrypt(session, file, directoryId, metadata);
    }

    @Override
    public <T> T getFeature(final Session<?> ignore, final Class<T> type, final T delegate) {
        log.debug("Delegate to {} for feature {}", storage, type);
        // Ignore feature implementation but delegate to storage backend
        return super.getFeature(storage, type, storage._getFeature(type));
    }

    @Override
    public synchronized void close() {
        try {
            log.debug("Close connection {}", storage);
            storage.close();
        }
        catch(BackgroundException e) {
            //
        }
        super.close();
    }

    /**
     * Upload vault template into existing bucket (permanent credentials)
     */
    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko check method signature?
    public synchronized Path create(final Session<?> session, final String region, final String metadata, final String hashedRootDirId) throws BackgroundException {
        log.debug("Uploading vault template {} in {} ", home, session.getHost());

        // N.B. there seems to be no API to check write permissions without actually writing.
        if(!session.getFeature(ListService.class).list(home, new DisabledListProgressListener()).isEmpty()) {
            throw new BackgroundException("Bucket not empty", String.format("Cannot upload bucket %s in %s is not empty.", home, session.getHost()));
        }

        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
        //        zip.file('vault.cryptomator', this.vaultConfigToken);
        //        zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
        new ContentWriter(session).write(new Path(home, PreferencesFactory.get().getProperty("cryptomator.vault.config.filename"), EnumSet.of(AbstractPath.Type.file, AbstractPath.Type.vault)), metadata.getBytes(StandardCharsets.US_ASCII));
        Directory<?> directory = (Directory<?>) session._getFeature(Directory.class);

        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 implement CryptoDirectory for uvf
        //        Path secondLevel = this.directoryProvider.toEncrypted(session, this.home.attributes().getDirectoryId(), this.home);
        final Path secondLevel = new Path(String.format("/%s/d/%s/%s/", session.getHost().getDefaultPath(), hashedRootDirId.substring(0, 2), hashedRootDirId.substring(2)), EnumSet.of(AbstractPath.Type.directory));
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
    public HubUVFVault load(final Session<?> ignore, final PasswordCallback prompt) throws BackgroundException {
        log.debug("Connect to {}", storage);
        storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
        storage.login(new DisabledLoginCallback(), new DisabledCancelCallback());
        super.load(storage, prompt);
        return this;
    }
}
