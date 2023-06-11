package me.alek.honey.listeners.check;

import me.alek.honey.listeners.impl.IllegalAttribute;
import me.alek.honey.listeners.impl.IllegalMovement;
import me.alek.honey.netty.api.example.InjectListener;

import java.util.function.Supplier;

public enum CheckRegistry {

    ILLEGAL_POSITION(IllegalMovement::new),
    ILLEGAL_ATTRIBUTE(IllegalAttribute::new),
    INJECTOR(InjectListener::new);

    private final Supplier<Check> check;

    CheckRegistry(Supplier<Check> check) {
        this.check = check;
    }

    public Supplier<Check> getCheck() {
        return check;
    }
}
