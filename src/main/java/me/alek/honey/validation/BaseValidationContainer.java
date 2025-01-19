package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;

public interface BaseValidationContainer {

    default void load() {

    }

    default void setup(HoneyPlayer player) {

    }

    default void cleanup(HoneyPlayer player) {

    }
}
