package me.alek.honey.validation;

import me.alek.honey.HoneyKey;
import me.alek.honey.configuration.ConfigurationOptions;

public interface KeyedVlValidation<T> extends ViolationValidation<T> {

    default ConfigurationOptions.ViolationSetting getSetting() {
        throw new UnsupportedOperationException();
    }

    default boolean isKeyed() {
        return true;
    }

    HoneyKey getKey(T val);

    ConfigurationOptions.ViolationSetting getSetting(T val);
}
