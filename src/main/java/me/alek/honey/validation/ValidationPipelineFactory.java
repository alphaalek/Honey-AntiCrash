package me.alek.honey.validation;

public interface ValidationPipelineFactory<T extends HoneyValidation<?>> {

    ValidationPipeline<T> createValidationPipeline();

    default boolean requiresPlayer() {
        return true;
    }
}
