package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;

public interface ViolationCheck extends HoneyCheck {

    ConfigurationOptions.VlSetting getSetting();

    default boolean isKeyed() {
        return false;
    }

    default boolean isViolation() {
        return true;
    }
}
