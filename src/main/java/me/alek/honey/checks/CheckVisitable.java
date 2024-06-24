package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.types.HoneyValidation;

public interface CheckVisitable<T extends HoneyValidation<?>> {

    CheckPipeline<T> getPipeline(HoneyPlayer player);

    void setInitializedCheckInstances();

    default boolean requiresPlayer() {
        return true;
    }
}
