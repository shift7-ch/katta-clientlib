/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Controller;
import ch.cyberduck.core.Factory;
import ch.cyberduck.core.FactoryException;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DeviceSetupCallbackFactory extends Factory<DeviceSetupCallback> {
    private static final Logger log = LogManager.getLogger(DeviceSetupCallbackFactory.class);

    public DeviceSetupCallbackFactory() {
        super("factory.devicesetupcallback.class");
    }

    public DeviceSetupCallback create(final Controller controller) {
        try {
            final Constructor<? extends DeviceSetupCallback> constructor
                    = ConstructorUtils.getMatchingAccessibleConstructor(clazz, controller.getClass());
            if(null == constructor) {
                log.warn("No matching constructor for parameter {}", controller.getClass());
                // Call default constructor for disabled implementations
                return clazz.getDeclaredConstructor().newInstance();
            }
            return constructor.newInstance(controller);
        }
        catch(InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.error("Failure loading callback class {}. {}", clazz, e.getMessage());
            throw new FactoryException(e.getMessage(), e);
        }
    }

    /**
     * @param c Window controller
     * @return Login controller instance for the current platform.
     */
    public static synchronized DeviceSetupCallback get(final Controller c) {
        return new DeviceSetupCallbackFactory().create(c);
    }
}
