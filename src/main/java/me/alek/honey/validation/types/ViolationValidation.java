package me.alek.honey.validation.types;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationServiceContext;
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

        default ValidationServiceContext validate(HoneyPlayer player, Integer amount, ValidationPipelineContext container) {
            return ValidationServiceContext.Builder.validate().flag(amount).build();
        }

        default boolean isDefaultPipeline() {
            return false;
        }
    }
}
