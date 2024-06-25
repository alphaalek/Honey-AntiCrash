package me.alek.honey.validation.types;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.CheckContextContainer;
import me.alek.honey.validation.response.ResponseContainer;

public interface HoneyValidation<T> {

    int getID();

    String getName();

    String getDescription();

    ResponseContainer validate(HoneyPlayer player, T type, CheckContextContainer container);

    default boolean isViolation() {
        return false;
    }
}
