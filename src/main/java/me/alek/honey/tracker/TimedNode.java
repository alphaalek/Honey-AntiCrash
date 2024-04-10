package me.alek.honey.tracker;

public class TimedNode<T> {

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
