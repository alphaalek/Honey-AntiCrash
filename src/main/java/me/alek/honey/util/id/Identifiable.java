package me.alek.honey.util.id;

public interface Identifiable<T> {

    T getID();

    @interface Name {

        String name();
    }
}
