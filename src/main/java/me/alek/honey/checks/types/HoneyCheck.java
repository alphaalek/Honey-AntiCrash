package me.alek.honey.checks.types;


public interface HoneyCheck {

    int getID();

    String getName();

    String getDescription();

    default boolean isViolation() {
        return false;
    }
}
