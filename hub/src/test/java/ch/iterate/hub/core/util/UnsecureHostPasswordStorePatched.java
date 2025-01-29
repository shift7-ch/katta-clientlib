/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core.util;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.UnsecureHostPasswordStore;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

import com.auth0.jwt.JWT;

/**
 * For testing with PasswordGrant, save OAuth credentials without username. Otherwise, vault session login is unable to retrieve shared OAuth credentials for getting the masterkey.
 * With AuthorizationCode, OAuth credentials are never stored with username.
 */
public class UnsecureHostPasswordStorePatched extends UnsecureHostPasswordStore {
    private static final Logger log = LogManager.getLogger(UnsecureHostPasswordStorePatched.class);

    protected static String[] getOAuthPrefix(final Host bookmark) {
//        if(StringUtils.isNotBlank(bookmark.getCredentials().getUsername())) {
//            return new String[]{
//                    String.format("%s (%s)", bookmark.getProtocol().getOAuthClientId(), bookmark.getCredentials().getUsername()),
//                    String.format("%s (%s)", bookmark.getProtocol().getDescription(), bookmark.getCredentials().getUsername())
//            };
//        }
        return new String[]{
                bookmark.getProtocol().getOAuthClientId(),
                bookmark.getProtocol().getDescription()
        };
    }

    // public for testing
    public static String getOAuthHostname(final Host bookmark) {
        final URI uri = URI.create(bookmark.getProtocol().getOAuthTokenUrl());
        if(StringUtils.isNotBlank(uri.getHost())) {
            return uri.getHost();
        }
        return bookmark.getHostname();
    }

    // public for testing
    public static int getOAuthPort(final Host bookmark) {
        final URI uri = URI.create(bookmark.getProtocol().getOAuthTokenUrl());
        if(-1 != uri.getPort()) {
            return uri.getPort();
        }
        return getOAuthScheme(bookmark).getPort();
    }

    // public for testing
    public static Scheme getOAuthScheme(final Host bookmark) {
        final URI uri = URI.create(bookmark.getProtocol().getOAuthTokenUrl());
        if(null == uri.getScheme()) {
            return bookmark.getProtocol().getScheme();
        }
        return Scheme.valueOf(uri.getScheme());
    }

    // copy-paste from DefaultHostPasswordStore to call patched getOAuthPrefix (as it is static)
    @Override
    public void save(final Host bookmark) {
        if(StringUtils.isEmpty(bookmark.getHostname())) {
            log.warn("No hostname given");
            return;
        }
        final Credentials credentials = bookmark.getCredentials();
        final Protocol protocol = bookmark.getProtocol();
        if(log.isInfoEnabled()) {
            log.info(String.format("Save credentials %s for bookmark %s", credentials, bookmark));
        }
        if(credentials.isPublicKeyAuthentication()) {
            this.addPassword(bookmark.getHostname(), credentials.getIdentity().getAbbreviatedPath(),
                    credentials.getIdentityPassphrase());
        }
        if(credentials.isPasswordAuthentication()) {
            if(StringUtils.isEmpty(credentials.getUsername())) {
                log.warn(String.format("No username in credentials for bookmark %s", bookmark.getHostname()));
                return;
            }
            if(StringUtils.isEmpty(credentials.getPassword())) {
                log.warn(String.format("No password in credentials for bookmark %s", bookmark.getHostname()));
                return;
            }
            this.addPassword(protocol.getScheme(), bookmark.getPort(),
                    bookmark.getHostname(), credentials.getUsername(), credentials.getPassword());
        }
        if(credentials.isTokenAuthentication()) {
            this.addPassword(protocol.getScheme(), bookmark.getPort(),
                    bookmark.getHostname(), StringUtils.isEmpty(credentials.getUsername()) ?
                            protocol.getTokenPlaceholder() : String.format("%s (%s)", protocol.getTokenPlaceholder(), credentials.getUsername()),
                    credentials.getToken());
        }
        if(credentials.isOAuthAuthentication()) {
            final String[] descriptors = getOAuthPrefix(bookmark);
            for(final String prefix : descriptors) {
                if(StringUtils.isNotBlank(credentials.getOauth().getAccessToken())) {
                    log.info(String.format("addPassword(%s,%s,%s)", getOAuthScheme(bookmark),
                            getOAuthPort(bookmark), getOAuthHostname(bookmark),
                            String.format("%s OAuth2 Access Token", prefix), credentials.getOauth().getAccessToken()));
                    this.addPassword(getOAuthScheme(bookmark),
                            getOAuthPort(bookmark), getOAuthHostname(bookmark),
                            String.format("%s OAuth2 Access Token", prefix), credentials.getOauth().getAccessToken());
                }
                if(StringUtils.isNotBlank(credentials.getOauth().getRefreshToken())) {
                    this.addPassword(getOAuthScheme(bookmark),
                            getOAuthPort(bookmark), getOAuthHostname(bookmark),
                            String.format("%s OAuth2 Refresh Token", prefix), credentials.getOauth().getRefreshToken());
                }
                // Save expiry
                if(credentials.getOauth().getExpiryInMilliseconds() != null) {
                    log.info(String.format("addPassword(%s,%s,%s)", getOAuthHostname(bookmark), String.format("%s OAuth2 Token Expiry", prefix),
                            String.valueOf(credentials.getOauth().getExpiryInMilliseconds())));
                    this.addPassword(getOAuthHostname(bookmark), String.format("%s OAuth2 Token Expiry", prefix),
                            String.valueOf(credentials.getOauth().getExpiryInMilliseconds()));
                }
                if(StringUtils.isNotBlank(credentials.getOauth().getIdToken())) {
                    this.addPassword(getOAuthScheme(bookmark),
                            getOAuthPort(bookmark), getOAuthHostname(bookmark),
                            String.format("%s OIDC Id Token", prefix), credentials.getOauth().getIdToken());
                }
                break;
            }
        }
    }

    // copy-paste from DefaultHostPasswordStore to call patched getOAuthPrefix (as it is static)
    @Override
    public OAuthTokens findOAuthTokens(final Host bookmark) {
        if(log.isInfoEnabled()) {
            log.info(String.format("XXX Fetching OAuth tokens from keychain for %s", bookmark));
        }
        final String[] descriptors = getOAuthPrefix(bookmark);
        for(final String prefix : descriptors) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Search with prefix %s", prefix));
            }
            final String hostname = getOAuthHostname(bookmark);
            if(log.isDebugEnabled()) {
                log.debug(String.format("Search with hostname %s", hostname));
            }
//            try {
            final String expiry = this.getPassword(getOAuthHostname(bookmark), String.format("%s OAuth2 Token Expiry", prefix));
            final OAuthTokens tokens = new OAuthTokens(
                    this.getPassword(getOAuthScheme(bookmark), getOAuthPort(bookmark), hostname,
                            String.format("%s OAuth2 Access Token", prefix)),
                    this.getPassword(getOAuthScheme(bookmark), getOAuthPort(bookmark), hostname,
                            String.format("%s OAuth2 Refresh Token", prefix)),
                    expiry != null ? Long.parseLong(expiry) : -1L,
                    this.getPassword(getOAuthScheme(bookmark), getOAuthPort(bookmark), hostname,
                            String.format("%s OIDC Id Token", prefix)));
            if(tokens.validate()) {
                if(log.isInfoEnabled()) {
                    log.info(String.format("XXX Returning OAuth tokens %s from keychain for %s", tokens, bookmark));
                    try {
                        log.info(String.format("XXX Returning OAuth access token with expiry %s in tokens %s from keychain for %s", JWT.decode(tokens.getAccessToken()).getExpiresAt(), tokens, bookmark));
                        log.info(String.format("XXX Returning OAuth refresh token with expiry %s in tokens %s from keychain for %s", JWT.decode(tokens.getRefreshToken()).getExpiresAt(), tokens, bookmark));
                        log.info(String.format("XXX Returning OAuth id token with expiry %s in tokens %s from keychain for %s", JWT.decode(tokens.getIdToken()).getExpiresAt(), tokens, bookmark));
                    }
                    catch(NullPointerException e) {
                        log.warn(String.format("Failure %s searching in keychain", e));
                        if(log.isInfoEnabled()) {
                            log.info(String.format("XXE Returning empty OAuth tokens from keychain for %s", bookmark));
                        }
                        log.warn(e);
                    }
                }
                if(log.isInfoEnabled()) {
                    log.info(String.format("XXE Returning empty OAuth tokens from keychain for %s", bookmark));
                }
                return tokens;
            }
            // Continue with deprecated descriptors
//            }
//            catch(LocalAccessDeniedException e) {
//                log.warn(String.format("Failure %s searching in keychain", e));
//                return OAuthTokens.EMPTY;
//            }
        }
        return OAuthTokens.EMPTY;
    }

    @Override
    public void addPassword(final String serviceName, final String accountName, final String password) {
        log.info(String.format("XXX %s, %s, %s", serviceName, accountName, password));
        super.addPassword(serviceName, accountName, password);
    }
}
