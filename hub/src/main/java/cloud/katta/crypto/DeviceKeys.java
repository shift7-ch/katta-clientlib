/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;

import javax.security.auth.Destroyable;
import java.util.Objects;

public class DeviceKeys implements Destroyable {

    private final ECKeyPair ecKeyPair;

    public DeviceKeys(final ECKeyPair ecKeyPair) {
        this.ecKeyPair = ecKeyPair;
    }

    public ECKeyPair getEcKeyPair() {
        return ecKeyPair;
    }

    @Override
    public void destroy() {
        ecKeyPair.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return ecKeyPair.isDestroyed();
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof DeviceKeys)) return false;

        DeviceKeys that = (DeviceKeys) o;
        return Objects.equals(ecKeyPair, that.ecKeyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ecKeyPair);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeviceKeys{");
        sb.append("ecKeyPair=").append(ecKeyPair);
        sb.append('}');
        return sb.toString();
    }

    public static DeviceKeys create() {
        return new DeviceKeys(P384KeyPair.generate());
    }

    public static boolean validate(final DeviceKeys deviceKeys) {
        return deviceKeys.getEcKeyPair() != null;
    }

    public static final DeviceKeys notfound = new DeviceKeys(null);
}
