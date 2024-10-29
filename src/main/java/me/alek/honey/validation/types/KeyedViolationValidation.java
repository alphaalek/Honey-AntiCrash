package me.alek.honey.validation.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.validation.ViolationManagement;

public interface KeyedViolationValidation<T> extends ViolationValidation<T> {

    default ConfigurationOptions.ViolationSetting getSetting() {
        throw new UnsupportedOperationException();
    }

    default boolean isKeyed() {
        return true;
    }

    ViolationManagement.HoneyKey getKey(T val);

    ConfigurationOptions.ViolationSetting getSetting(T val);
}
