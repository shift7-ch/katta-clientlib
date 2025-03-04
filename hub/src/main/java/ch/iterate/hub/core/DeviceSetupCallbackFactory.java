/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Factory;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class DeviceSetupCallbackFactory extends Factory<DeviceSetupCallback> {
    private static final Logger log = LogManager.getLogger(DeviceSetupCallbackFactory.class);

    private DeviceSetupCallbackFactory() {
        super("factory.devicesetupcallback.class");
    }

    public DeviceSetupCallback create() {
        try {
            final Constructor<? extends DeviceSetupCallback> constructor
                    = ConstructorUtils.getMatchingAccessibleConstructor(clazz);
            if(null == constructor) {
                log.warn("No default controller in {}", constructor.getClass());
                // Call default constructor for disabled implementations
                return clazz.getDeclaredConstructor().newInstance();
            }
            return constructor.newInstance();
        }
        catch(InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.error("Failure loading callback class {}. {}", clazz, e.getMessage());
            return DeviceSetupCallback.disabled;
        }
    }

    private static DeviceSetupCallbackFactory singleton;

    /**
     * @return Firs tLogin Device Setup Callback instance for the current platform.
     */
    public static synchronized DeviceSetupCallback get() {
        if(null == singleton) {
            singleton = new DeviceSetupCallbackFactory();
        }
        return singleton.create();
    }
}
