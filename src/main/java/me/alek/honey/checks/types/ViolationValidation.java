package me.alek.honey.checks.types;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.configuration.ConfigurationOptions;

public interface ViolationValidation<T> extends HoneyValidation<T> {

    ConfigurationOptions.VlSetting getSetting();

    default boolean isViolation() {
        return true;
    }

    default boolean isKeyed() {
        return false;
    }

    interface IntViolation extends ViolationValidation<Integer> {

        default ResponseContainer validate(HoneyPlayer player, Integer type, CheckContextContainer container) {
            return Validation.validate().flag(type).build();
        }
    }
}
