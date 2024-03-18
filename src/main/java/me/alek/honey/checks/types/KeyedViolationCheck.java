package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.packetwrappers.WrappedPacket;

public interface KeyedViolationCheck<WP extends WrappedPacket<WP>, T> extends ViolationCheck {

    default ConfigurationOptions.VlSetting getSetting() {
        return null;
    }

    ConfigurationOptions.VlSetting getSetting(WP packet);

    T getKey(WP packet);

    default boolean isKeyed() {
        return true;
    }

}
