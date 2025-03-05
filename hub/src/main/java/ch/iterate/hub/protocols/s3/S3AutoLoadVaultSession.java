/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.s3.RequestEntityRestStorageService;
import ch.cyberduck.core.shared.DefaultPathHomeFeature;
import ch.cyberduck.core.shared.DelegatingHomeFeature;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.DeviceKeysServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;

public class S3AutoLoadVaultSession extends S3AssumeRoleSession {
    private static final Logger log = LogManager.getLogger(S3AutoLoadVaultSession.class);

    private final AbstractHostCollection bookmarks = BookmarkCollection.defaultCollection();
    private final HostPasswordStore keychain = PasswordStoreFactory.get();

    private HubSession backend;

    public S3AutoLoadVaultSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    @Override
    public RequestEntityRestStorageService open(final ProxyFinder proxy, final HostKeyCallback hostcallback, final LoginCallback login, final CancelCallback cancel) throws BackgroundException {
        final Host hub = bookmarks.lookup(host.getProperty(HubSession.HUB_UUID));
        if(null == hub) {
            throw new ConnectionCanceledException(String.format("Missing configuration %s", host.getProperty(HubSession.HUB_UUID)));
        }
        backend = new HubSession(hub, trust, key);
        backend.open(proxy, hostcallback, login, cancel);
        return super.open(proxy, hostcallback, login, cancel);
    }

    /**
     * Unlock vault using universal vault format metadata payload after verifying credentials
     */
    @Override
    public void login(final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        try {
            final Credentials credentials = backend.getHost().getCredentials().withOauth(keychain.findOAuthTokens(backend.getHost()));
            log.debug("Verify credentials {} with {}", credentials, backend);
            backend.login(prompt, cancel);
            log.debug("Verify credentials {} with {}", host.getCredentials(), host);
            super.login(prompt, cancel);
            final Path home = new DelegatingHomeFeature(new DefaultPathHomeFeature(host)).find();
            log.debug("Attempting to locate vault in {}", home);
            // Retrieve from keychain
            final DeviceKeys deviceKeys = new DeviceKeysServiceImpl(keychain).getDeviceKeys(backend.getHost());
            final UvfMetadataPayload vaultMetadata = new VaultServiceImpl(backend).getVaultMetadataJWE(
                    UUID.fromString(host.getUuid()), new UserKeysServiceImpl(backend).getUserKeys(backend.getHost(), backend.getMe(), deviceKeys));
            final String decryptedPayload = vaultMetadata.toJSON();
            final Vault vault = VaultFactory.get(home, decryptedPayload, "", new byte[0]);
            registry.add(vault.load(this, new DisabledPasswordCallback() {
                @Override
                public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                    return new VaultCredentials(decryptedPayload);
                }
            }));
            backend.close();
        }
        catch(ApiException | SecurityFailure | AccessException | JsonProcessingException e) {
            throw new LoginFailureException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
    }
}
