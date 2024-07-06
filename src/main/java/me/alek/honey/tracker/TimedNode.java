package me.alek.honey.tracker;

import me.alek.honey.Identifiable;

public class TimedNode<T> implements Identifiable {

    public TimedNode(T value, int identifier, long insertionInstant) {
        this.value = value;
        this.identifier = identifier;
        this.insertionInstant = insertionInstant;
    }

    public TimedNode(T value, int identifier) {
        this(value, identifier, System.nanoTime());
    }

    public TimedNode(T value) {
        this(value, -1);
    }

    private final T value;
    private final int identifier;
    private final long insertionInstant;

    public T getValue() {
        return value;
    }

    public int getId() {
        return identifier;
    }

    public long getInsertionInstant() {
        return insertionInstant;
    }
}
