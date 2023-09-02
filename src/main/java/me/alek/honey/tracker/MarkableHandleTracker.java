package me.alek.honey.tracker;

public interface MarkableHandleTracker<I extends Number, T> extends HandleTracker<I, T> {

    default void registerTime(T key, I value) {
        throw new UnsupportedOperationException("You have to use registerTimeStart() and registerTimeStop() in MarkableHandleTracker");
    }

    void registerTimeStart(T key);

    void registerTimeStop(T key);
}
