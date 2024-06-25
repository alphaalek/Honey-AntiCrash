package me.alek.honey.validation.types;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.ValidationContextContainer;
import me.alek.honey.validation.ResponseContainer;
import me.alek.honey.validation.Validation;
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

        default ResponseContainer validate(HoneyPlayer player, Integer type, ValidationContextContainer container) {
            return Validation.validate().flag(type).build();
        }
    }
}
