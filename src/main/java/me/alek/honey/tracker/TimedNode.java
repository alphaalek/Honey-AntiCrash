package me.alek.honey.tracker;

import java.io.Serializable;

public class TimedNode<T> implements Serializable {

    private static final long serialVersionUID = 1210079567439681068L;

    public TimedNode(T value, long identifier, long insertionInstant) {
        this.value = value;
        this.identifier = identifier;
        this.insertionInstant = insertionInstant;
    }

    public TimedNode(T value, long identifier) {
        this(value, identifier, System.nanoTime());
    }

    public TimedNode(T value) {
        this(value, -1);
    }

    private final T value;
    private final long identifier;
    private final long insertionInstant;

    public T getValue() {
        return value;
    }

    public long getID() {
        return identifier;
    }

    public long getInsertionInstant() {
        return insertionInstant;
    }
}
