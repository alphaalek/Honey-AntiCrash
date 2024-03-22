package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;

public interface KeyedViolationCheck<T> extends ViolationCheck {

    default ConfigurationOptions.VlSetting getSetting() {
        throw new UnsupportedOperationException();
    }

    default boolean isKeyed() {
        return true;
    }

    ConfigurationOptions.VlSetting getSetting(T val);
}
