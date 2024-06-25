package me.alek.honey.validation.types;


import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.ValidationContextContainer;
import me.alek.honey.validation.response.ResponseContainer;

public interface HoneyValidation<T> {

    int getID();

    String getName();

    String getDescription();

    ResponseContainer validate(HoneyPlayer player, T type, ValidationContextContainer container);

    default boolean isViolation() {
        return false;
    }
}
