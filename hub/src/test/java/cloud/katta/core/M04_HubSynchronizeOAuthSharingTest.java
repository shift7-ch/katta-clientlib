/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;

import cloud.katta.testsetup.AbstractHubTest;

// TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 (*) get M04 running again
/**
 * Get all bookmarks from hub and verify OAuth tokens.
 */
@Disabled("run standalone against already running hub")
public class M04_HubSynchronizeOAuthSharingTest extends AbstractHubTest {

    private static final Logger log = LogManager.getLogger(M04_HubSynchronizeOAuthSharingTest.class.getName());

//    @Override
//    public void hubTest(final HubTestConfig hubTestConfig, final HubSession hubSession) throws Exception {
//        final HubTestSetupConfig hubTestSetupConfig = hubTestConfig.hubTestSetupConfig;
//        log.info(String.format("M04 %s", hubTestSetupConfig));
//
//        // wait for first sync
//        Thread.sleep(500);
//
//        final VaultResourceApi vaultResourceApi = new VaultResourceApi(hubSession.getClient());
//        final List<VaultDto> vaults = vaultResourceApi.apiVaultsAccessibleGet(null);
//
//        List<Host> bookmarks = new ArrayList<>();
//        for(VaultDto vault : vaults) {
//            if(vault.getArchived()) {
//                continue;
//            }
//            try {
//                bookmarks.add(new VaultProfileBookmarkService(hubSession).getVaultBookmark(vault.getId()));
//            }
//            catch(ApiException e) {
//                // bookmarks not archivable after account reset
//            }
//        }
//        for(final Host bookmark : bookmarks) {
//            log.info("==================================================================");
//            log.info(bookmark.getUuid());
//            log.info(bookmark.getNickname());
//            log.info("==================================================================");
//            //  CASE 1: verify that vault login does not change the stored OAuth credentials from hub session
//            {
//                log.info("CASE 1: verify that vault login does not change the stored OAuth credentials from hub session");
//                // populate passwordstore
////                this.setupForUser(hubTestSetupConfig, hubTestSetupConfig.USER_001());
//
//                final Host hubBookmark = hubSession.getHost();
//                final OAuthTokens oauthTokensAfterHubSessionLogin = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
//
//                // do a vault login fetching by fetching the OAuth credentials from the keychain (required for fetching masterkey from hub and opt. for STS)
//                final Session<?> vaultSession = vaultLoginWithSharedOAuthCredentialsFromPasswordStore(bookmark);
//                final OAuthTokens oauthTokensAfterVaultLogin = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
//
//                final Long expiry = oauthTokensAfterVaultLogin.getExpiryInMilliseconds();
//                final long now = Timestamp.now().getMillis();
//                assertTrue(expiry > now, String.format("%s %s", expiry, now));
//                assertTrue(expiry < Long.MAX_VALUE);
//                assertEquals(oauthTokensAfterHubSessionLogin.getExpiryInMilliseconds(), expiry);
//                assertEquals(oauthTokensAfterHubSessionLogin.getAccessToken(), oauthTokensAfterVaultLogin.getAccessToken());
//                assertEquals(oauthTokensAfterHubSessionLogin.getRefreshToken(), oauthTokensAfterVaultLogin.getRefreshToken());
//                assertEquals(oauthTokensAfterHubSessionLogin.getIdToken(), oauthTokensAfterVaultLogin.getIdToken());
//
//                AttributedList<Path> list = vaultSession.getFeature(ListService.class).list(new Path(String.format("/%s", bookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
//                log.debug("paths:");
//                for(Path path : list) {
//                    log.debug(String.format("%s", path));
//                }
//                assert list.contains(new Path(String.format("/%s/%s", bookmark.getDefaultPath(), PreferencesFactory.get().getProperty("cryptomator.vault.config.filename")), EnumSet.of(AbstractPath.Type.file)));
//            }
//
//            //  CASE 2: verify forced refresh upon vault login
//            {
//                log.info("CASE 2: verify forced refresh upon vault login");
//                // populate passwordstore
////                this.setupForUser(hubTestSetupConfig, hubTestSetupConfig.USER_001());
//
//                final Host hubBookmark = hubSession.getHost();
//                final OAuthTokens oauthTokensAfterHubSessionLogin = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
//
//                // do a vault login fetching by fetching the OAuth credentials from the keychain (required for fetching masterkey from hub and opt. for STS)
//                log.info("M04 invalidate");
//                PasswordStoreFactory.get().addPassword(getOAuthHostname(hubBookmark), "cryptomator OAuth2 Token Expiry", Long.toString(System.currentTimeMillis() - 10000));
//                log.info("M04 new vault login");
//                final Session<?> vaultSession = vaultLoginWithSharedOAuthCredentialsFromPasswordStore(bookmark);
//                final OAuthTokens oauthTokensAfterVaultLogin = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
//                final Long expiry = oauthTokensAfterVaultLogin.getExpiryInMilliseconds();
//                final long now = Timestamp.now().getMillis();
//                assertTrue(expiry > now, String.format("%s %s", expiry, now));
//                assertTrue(expiry < Long.MAX_VALUE);
//                log.info("M04 after new vault login");
//                log.info(oauthTokensAfterHubSessionLogin);
//                log.info(oauthTokensAfterHubSessionLogin.getAccessToken());
//                log.info(oauthTokensAfterHubSessionLogin.getRefreshToken());
//                log.info(oauthTokensAfterHubSessionLogin.getIdToken());
//                log.info(oauthTokensAfterHubSessionLogin.getExpiryInMilliseconds());
//                log.info(expiry);
//                assertNotEquals(oauthTokensAfterHubSessionLogin.getExpiryInMilliseconds(), expiry);
//                assertNotEquals(oauthTokensAfterHubSessionLogin.getAccessToken(), oauthTokensAfterVaultLogin.getAccessToken());
//                assertNotEquals(oauthTokensAfterHubSessionLogin.getRefreshToken(), oauthTokensAfterVaultLogin.getRefreshToken());
//                assertNotEquals(oauthTokensAfterHubSessionLogin.getIdToken(), oauthTokensAfterVaultLogin.getIdToken());
//
//                AttributedList<Path> list = vaultSession.getFeature(ListService.class).list(new Path(String.format("/%s", bookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
//                log.debug("paths:");
//                for(Path path : list) {
//                    log.debug(String.format("%s", path));
//                }
//                assert list.contains(new Path(String.format("/%s/%s", bookmark.getDefaultPath(), PreferencesFactory.get().getProperty("cryptomator.vault.config.filename")), EnumSet.of(AbstractPath.Type.file)));
//            }
//
//            // CASE 3: when the hub session has done a new OAuth flow (the storage session cannot refresh any more, will the storage session refresh from Keychain?
//            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 @dko argues this is not a problem ("Ferienfall") - can we really live with it? There is an aysmmetry in the implementation:
//            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 what are the default session times in Keycloack?
//            // 2024-01-23 Diskussion:
//            // - yla: Loginallback implementieren?
//            // - dko: disconnect löst das Problem
//            // - ce refreshte tokens zurückschreiben aus verschiedenen storage sessions - ist das kein Problem?
//
//            {
////                // populate passwordstore
////                this.setupForUser(hubTestSetupConfig, hubTestSetupConfig.USER_001());
////
////                final Host hubBookmark = hubSession.getHost();
////                // wait for sync
////                Thread.sleep(1000);
////
////                final OAuthTokens sharedOAuthTokensAfterHubLogin = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
////
////                // do a vault login fetching by fetching the OAuth credentials from the keychain (required for fetching masterkey from hub and opt. for STS)
////                log.info("tweak keychain and vault session");
////
////                final long tweakedExpiry = System.currentTimeMillis() + 5000;
////                PasswordStoreFactory.get().addPassword(getOAuthHostname(hubBookmark), "cryptomator OAuth2 Token Expiry", Long.toString(tweakedExpiry));
////                //PasswordStoreFactory.get().addPassword(getOAuthScheme(hubBookmark), getOAuthPort(hubBookmark), getOAuthHostname(hubBookmark), "cryptomator OAuth2 Refresh Token", "");
////                //PasswordStoreFactory.get().addPassword(getOAuthScheme(hubBookmark), getOAuthPort(hubBookmark), getOAuthHostname(hubBookmark), "cryptomator OAuth2 Access Token", "");
////
////                final Session<?> vaultSession = vaultLoginWithSharedOAuthCredentialsFromPasswordStore(bookmark);
////                assertEquals(sharedOAuthTokensAfterHubLogin.getAccessToken(),vaultSession.getHost().getCredentials().getOauth().getAccessToken());
////                assertEquals(tweakedExpiry,vaultSession.getHost().getCredentials().getOauth().getExpiryInMilliseconds());
////
////                Thread.sleep(5000);
////                assertTrue(System.currentTimeMillis() > tweakedExpiry);
////
////                // populate anew
////                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 very clumsy!
////                this.setupForUser(hubTestSetupConfig, hubTestSetupConfig.USER_001());
////                final OAuthTokens oauthTokensAfterRePopulation = PasswordStoreFactory.get().findOAuthTokens(hubBookmark);
////                final Long expiry = oauthTokensAfterRePopulation.getExpiryInMilliseconds();
////                final long now = Timestamp.now().getMillis();
////                assertTrue(expiry > now, String.format("%s %s", expiry, now));
////                assertTrue(expiry < Long.MAX_VALUE);
////                assertNotEquals(sharedOAuthTokensAfterHubLogin.getExpiryInMilliseconds(), oauthTokensAfterRePopulation.getExpiryInMilliseconds());
////                assertNotEquals(sharedOAuthTokensAfterHubLogin.getRefreshToken(), oauthTokensAfterRePopulation.getRefreshToken());
////                assertNotEquals(sharedOAuthTokensAfterHubLogin.getAccessToken(), oauthTokensAfterRePopulation.getAccessToken());
////                assertNotEquals(sharedOAuthTokensAfterHubLogin.getIdToken(), oauthTokensAfterRePopulation.getIdToken());
////
////
////                log.info("check vault session to picks up new credentials again in listing");
////                AttributedList<Path> list = vaultSession.getFeature(ListService.class).list(new Path(String.format("/%s", bookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
////
////                assertTrue(vaultSession.getHost().getCredentials().getOauth().getExpiryInMilliseconds() >= tweakedExpiry);
////                assertTrue(vaultSession.getHost().getCredentials().getOauth().getExpiryInMilliseconds() > sharedOAuthTokensAfterHubLogin.getExpiryInMilliseconds());
////                log.info("paths:");
////                for(Path path : list) {
////                    log.info(String.format("%s", path));
////                }
////                assert list.contains(new Path(String.format("/%s/vault.uvf", bookmark.getDefaultPath()), EnumSet.of(AbstractPath.Type.file)));
//            }
//        }
//        log.info(String.format("Listed %s vaults: %s", bookmarks.size(), bookmarks));
//    }
}
