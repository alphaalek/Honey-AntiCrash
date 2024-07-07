package me.alek.honey.validation.types;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.util.Identifiable;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationServiceContext;

public interface HoneyValidation<T> extends Identifiable {

    String getName();

    String getDescription();

    ValidationServiceContext validate(HoneyPlayer player, T type, ValidationPipelineContext container);

    default boolean isViolation() {
        return false;
    }

    default boolean isDefaultPipeline() {
        return true;
    }
}
