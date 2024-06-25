package me.alek.honey.validation.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.services.ViolationService;

public interface KeyedViolationValidation<T> extends ViolationValidation<T> {

    default ConfigurationOptions.VlSetting getSetting() {
        throw new UnsupportedOperationException();
    }

    default boolean isKeyed() {
        return true;
    }

    ViolationService.HoneyKey getKey(T val);

    ConfigurationOptions.VlSetting getSetting(T val);
}
