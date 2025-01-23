package me.alek.honey.validation.types;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.util.Identifiable;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationViolationContext;
import me.alek.honey.validation.ViolationKey;

public interface HoneyValidation<T> extends Identifiable {

    default ViolationKey generateKey() {
        return ViolationKey.of(this);
    }

    String getName();

    String getDescription();

    ValidationViolationContext validate(HoneyPlayer player, T type, ValidationPipelineContext context);

    default boolean isViolation() {
        return false;
    }
}
