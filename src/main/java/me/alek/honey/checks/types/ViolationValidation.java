package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;

public interface ViolationValidation<T> extends HoneyValidation<T> {

    ConfigurationOptions.VlSetting getSetting();

    default boolean isViolation() {
        return true;
    }

    default boolean isKeyed() {
        return false;
    }
}
