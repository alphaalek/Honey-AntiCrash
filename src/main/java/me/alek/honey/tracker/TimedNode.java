package me.alek.honey.tracker;

public class TimedNode<T> {

    public TimedNode(T value, long identifier) {
        this.value = value;
        this.identifier = identifier;
    }

    private final T value;
    private final long identifier;
    private final long insertionInstant = System.nanoTime();

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
