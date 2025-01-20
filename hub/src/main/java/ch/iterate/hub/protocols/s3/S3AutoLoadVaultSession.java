/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.AbstractController;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.shared.DefaultPathHomeFeature;
import ch.cyberduck.core.shared.DelegatingHomeFeature;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.threading.MainAction;
import ch.cyberduck.core.vault.VaultFactory;

import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.google.common.primitives.Bytes;
import com.nimbusds.jose.util.Base64URL;

import static ch.iterate.hub.protocols.hub.HubSession.createFromHubUrl;
import static ch.iterate.hub.protocols.s3.CipherduckHostCustomProperties.HUB_URL;
import static ch.iterate.hub.protocols.s3.CipherduckHostCustomProperties.HUB_USERNAME;

public class S3AutoLoadVaultSession extends S3Session {

    public S3AutoLoadVaultSession(final Host host) {
        super(host);
    }

    public S3AutoLoadVaultSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    public S3AutoLoadVaultSession(final Host host, final String authorization) {
        super(host);
    }

    public S3AutoLoadVaultSession(final Host host, final X509TrustManager trust, final X509KeyManager key, final String authorization) {
        super(host, trust, key);
    }

    @Override
    public void login(LoginCallback prompt, CancelCallback cancel) throws BackgroundException {
        // if an authorizationCode OAuth flow is triggered, this fails with NullPointerException as redirectUri is null
        try {
            super.login(prompt, cancel);
        }
        catch(NullPointerException e) {
            throw new LoginFailureException(LocaleFactory.localizedString("Login failed - OAuth session not open", "Credentials"), e);
        }
        try {
            // 2023-12-22 discussion/decision dko+ChE:
            // AuthorizationCode OAuth tokens are stored without username as username distinction only implemented in STSAssumeRoleCredentialsRequestInterceptor, but not in OAuth2RequestInterceptor.
            // Unclear why it was introduced in STSAssumeRoleCredentialsRequestInterceptor but not in OAuth2RequestInterceptor (both get an ID token, so it would be possible to use the same "meccano").
            // Decision: We do not support PasswwordGrant not supported (implement workaround for integration testing).
            // Decision: Keep vault sessions self-contained through OAuth token-sharing in Keychain and storing Hub URL in bookmark.
            //           I.e. no injection of a shared HubSession by drilling up SessionFactory in core.

            final String hubURL = getHost().getProperty(HUB_URL);
            final String hubUsername = getHost().getProperty(HUB_USERNAME);
            final HubSession hubSession = createFromHubUrl(hubURL, hubUsername, new AbstractController() {
                @Override
                public void invoke(final MainAction runnable, final boolean wait) {
                    // controller used for trust and key manager for hub access. At this point, we assume this is all right.
                }
            });

            final UserKeysServiceImpl userKeysService = new UserKeysServiceImpl(hubSession);
            final VaultServiceImpl vaultService = new VaultServiceImpl(hubSession);
            final UvfMetadataPayload vaultMetadata = vaultService.getVaultMetadataJWE(
                    UUID.fromString(host.getUuid()), userKeysService.getUserKeys(host, FirstLoginDeviceSetupCallbackFactory.get()));
            final Path home = new DelegatingHomeFeature(new DefaultPathHomeFeature(host)).find();

            // as in VaultFinderListProgressListener:
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 harmonize interface? Should we pass in full uvf metadata?
            final Vault vault = VaultFactory.get(home);
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MUST NEVER BE RELEASED LIKE THIS
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 use rawFileKey,rawNameKey as vault key for now (going into cryptolib's Masterkey)
            byte[] rawFileKey = Base64URL.from((vaultMetadata.seeds().get(vaultMetadata.latestSeed()))).decode();
            byte[] rawNameKey = Base64URL.from((vaultMetadata.seeds().get(vaultMetadata.latestSeed()))).decode();
            final byte[] vaultKey = Bytes.concat(rawFileKey, rawNameKey);

            // as in LoadingVaultLookupListener:
            registry.add(vault.load(this, new PasswordCallback() {
                @Override
                public void close(final String input) {
                    // nothing to do
                }

                @Override
                public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                    return new Credentials().withPassword(Base64.getEncoder().encodeToString(vaultKey));
                }
            }));
        }
        catch(ApiException | SecurityFailure | AccessException e) {
            // make sure we never display the encrypted files (vault.cryptomator, / d/....)
            throw new LoginFailureException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
    }
}
