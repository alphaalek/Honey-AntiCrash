package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.services.ViolationService;

public interface KeyedViolationCheck<T> extends ViolationCheck {

    default ConfigurationOptions.VlSetting getSetting() {
        throw new UnsupportedOperationException();
    }

    default boolean isKeyed() {
        return true;
    }

    ViolationService.HoneyKey getKey(T val);

    ConfigurationOptions.VlSetting getSetting(T val);
}
