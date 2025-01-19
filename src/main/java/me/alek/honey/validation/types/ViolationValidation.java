package me.alek.honey.validation.types;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationViolationContext;
import me.alek.honey.configuration.ConfigurationOptions;

public interface ViolationValidation<T> extends HoneyValidation<T> {

    ConfigurationOptions.ViolationSetting getSetting();

    default boolean isViolation() {
        return true;
    }

    default boolean isKeyed() {
        return false;
    }

    interface IntViolation extends ViolationValidation<Integer> {

        default ValidationViolationContext validate(HoneyPlayer player, ValidationPipelineContext context) {
            return validate(player, 1, context);
        }

        default ValidationViolationContext validate(HoneyPlayer player, Integer amount, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(amount).build();
            }

            return ValidationViolationContext.PASS;
        }

        default boolean isDefaultPipeline() {
            return false;
        }
    }
}
