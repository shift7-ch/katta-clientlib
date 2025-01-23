/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub.serializer;

import ch.cyberduck.core.serializer.Deserializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProxyDeserializer<T> implements Deserializer<T> {
    private static final Logger log = LogManager.getLogger(ProxyDeserializer.class);

    private final Deserializer<T> proxy;

    public ProxyDeserializer(final Deserializer<T> proxy) {
        this.proxy = proxy;
    }

    @Override
    public String stringForKey(final String key) {
        return proxy.stringForKey(key);
    }

    @Override
    public T objectForKey(final String key) {
        return proxy.objectForKey(key);
    }

    @Override
    public <L> List<L> listForKey(final String key) {
        return proxy.listForKey(key);
    }

    @Override
    public Map<String, String> mapForKey(final String key) {
        return proxy.mapForKey(key);
    }

    @Override
    public boolean booleanForKey(final String key) {
        return proxy.booleanForKey(key);
    }

    @Override
    public List<String> keys() {
        return proxy.keys();
    }

    public static <T> Deserializer<T> empty() {
        return new Deserializer<T>() {
            @Override
            public String stringForKey(final String key) {
                log.warn("Unknown key {}", key);
                return null;
            }

            @Override
            public T objectForKey(final String key) {
                log.warn("Unknown key {}", key);
                return null;
            }

            @Override
            public <L> List<L> listForKey(final String key) {
                log.warn("Unknown key {}", key);
                return Collections.emptyList();
            }

            @Override
            public Map<String, String> mapForKey(final String key) {
                log.warn("Unknown key {}", key);
                return Collections.emptyMap();
            }

            @Override
            public boolean booleanForKey(final String key) {
                log.warn("Unknown key {}", key);
                return false;
            }

            @Override
            public List<String> keys() {
                return Collections.emptyList();
            }
        };
    }
}
