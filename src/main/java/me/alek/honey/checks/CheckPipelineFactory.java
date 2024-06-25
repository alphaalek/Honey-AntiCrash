package me.alek.honey.checks;

import me.alek.honey.checks.types.HoneyValidation;

public interface CheckPipelineFactory<T extends HoneyValidation<?>> {

    CheckPipeline<T> getNewPipeline();

    default boolean requiresPlayer() {
        return true;
    }
}
