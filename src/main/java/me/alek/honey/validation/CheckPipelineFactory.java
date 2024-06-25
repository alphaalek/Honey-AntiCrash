package me.alek.honey.validation;

import me.alek.honey.validation.types.HoneyValidation;

public interface CheckPipelineFactory<T extends HoneyValidation<?>> {

    CheckPipeline<T> getNewPipeline();

    default boolean requiresPlayer() {
        return true;
    }
}
