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

public class FirstLoginDeviceSetupCallbackFactory extends Factory<FirstLoginDeviceSetupCallback> {
    private static final Logger log = LogManager.getLogger(FirstLoginDeviceSetupCallbackFactory.class);

    private FirstLoginDeviceSetupCallbackFactory() {
        super("factory.firstlogindevicesetupcallback.class");
    }

    public FirstLoginDeviceSetupCallback create() {
        try {
            final Constructor<? extends FirstLoginDeviceSetupCallback> constructor
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
            return FirstLoginDeviceSetupCallback.disabled;
        }
    }

    private static FirstLoginDeviceSetupCallbackFactory singleton;

    /**
     * @return Firs tLogin Device Setup Callback instance for the current platform.
     */
    public static synchronized FirstLoginDeviceSetupCallback get() {
        if(null == singleton) {
            singleton = new FirstLoginDeviceSetupCallbackFactory();
        }
        return singleton.create();
    }
}
