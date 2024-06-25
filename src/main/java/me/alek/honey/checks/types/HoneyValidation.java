package me.alek.honey.checks.types;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;

public interface HoneyValidation<T> {

    int getID();

    String getName();

    String getDescription();

    ResponseContainer validate(HoneyPlayer player, T type, CheckContextContainer container);

    default boolean isViolation() {
        return false;
    }
}
