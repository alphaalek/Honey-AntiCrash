package me.alek.honey.validation;

import me.alek.honey.validation.types.HoneyValidation;

public interface ValidationPipelineFactory<T extends HoneyValidation<?>> {

    ValidationPipeline<T> createValidationPipeline();

    default boolean requiresPlayer() {
        return true;
    }
}
