/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.serializer.Deserializer;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.model.ConfigDto;
import com.dd.plist.NSDictionary;

import static ch.cyberduck.core.Profile.*;
import static ch.cyberduck.core.ProtocolFactory.DEFAULT_PROTOCOL_PREDICATE;
import static ch.iterate.hub.protocols.hub.HubSession.getPortFromHubURI;

public class HubProfileBookmarkService {

    /**
     * Make profile <code>Profile</code> from config retrieved from hub and hub URL.
     *
     * @return
     * @throws ApiException
     * @throws IOException
     */
    public Profile makeHubProfile(final String hubURL, final ConfigDto hubConfig) throws ApiException, IOException {
        final URI hubURI = URI.create(hubURL);

        // search in default registered protocol specification as parent but not other profile
        // decision 2024-03-13 dko+CE: postpone refactoring generic Profile constructor (builder pattern) to avoid implementing Deserializer here
        final Profile hubProfile = new Profile(ProtocolFactory.get().forName(ProtocolFactory.get().find(DEFAULT_PROTOCOL_PREDICATE), "hub", null), new Deserializer<NSDictionary>() {
            @Override
            public String stringForKey(final String s) {
                switch(s) {
                    case PROTOCOL_KEY:
                        return "hub";
                    case VENDOR_KEY:
                        // Provider: We provide Scheme-specific profiles in Cipherduck client
                        // Bookmark has Provider field to match up with Vendor in Profile!
                        return hubConfig.getUuid();
                    case SCHEME_KEY:
                        return hubURI.getScheme();
                    case OAUTH_CLIENT_ID_KEY:
                        // We use client_id="cryptomator" in cipherduck, see discussion https://github.com/chenkins/cipherduck-hub/issues/6
                        return hubConfig.getKeycloakClientIdCryptomator();
                    case DEFAULT_PORT_KEY:
                        return String.valueOf(getPortFromHubURI(hubURI));
                    case DEFAULT_HOSTNAME_KEY:
                        return hubURI.getHost();
                    case DESCRIPTION_KEY:
                        return String.format("Cipherduck profile for hub %s (%s)", hubURL, hubConfig.getUuid());
                    case OAUTH_AUTHORIZATION_URL_KEY:
                        return hubConfig.getKeycloakAuthEndpoint();
                    case OAUTH_TOKEN_URL_KEY:
                        return hubConfig.getKeycloakTokenEndpoint();
                    case DEFAULT_PATH_KEY:
                        hubURI.getPath();
                }
                return null;
            }

            @Override
            public NSDictionary objectForKey(final String s) {
                return null;
            }

            @Override
            public <L> List<L> listForKey(final String s) {
                return null;
            }

            @Override
            public Map<String, String> mapForKey(final String s) {
                return null;
            }

            @Override
            public boolean booleanForKey(final String s) {
                return false;
            }

            @Override
            public List<String> keys() {
                return Arrays.asList(
                        PROTOCOL_KEY,
                        VENDOR_KEY,
                        SCHEME_KEY,
                        OAUTH_CLIENT_ID_KEY,
                        DEFAULT_PORT_KEY,
                        DEFAULT_HOSTNAME_KEY,
                        DESCRIPTION_KEY,
                        OAUTH_AUTHORIZATION_URL_KEY,
                        OAUTH_TOKEN_URL_KEY,
                        DEFAULT_PATH_KEY
                );
            }
        });
        return hubProfile;
    }

    /**
     * Make hub bookmark from profile, hub URL and hub UUID.
     *
     * @param profile
     * @param hubURL
     * @param hubUUID
     * @return
     */
    public Host makeHubBookmark(final Protocol profile, final String hubURL, final String hubUUID) {
        final Host host = new Host(profile);
        // allows to distinguish between hub and vault bookmarks in Cipherduck
        host.setProperty("fs.sync.mode", "none");
        // we have a bookmark and profile for the same hub UUID
        host.setUuid(hubUUID);
        host.setNickname(String.format("Cipherduck (%s)", hubURL));
        return host;
    }
}
