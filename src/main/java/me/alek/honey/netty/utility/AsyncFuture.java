package me.alek.honey.netty.utility;

import java.util.*;
import java.util.function.Consumer;

public class AsyncFuture {

    private final List<Runnable> listeners = new ArrayList<>();
    private final Queue<Consumer<AsyncFuture>> waitingFutures = new LinkedList<>();
    private boolean isDone = false;

    public AsyncFuture() {

    }

    public AsyncFuture(Runnable... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }

    public AsyncFuture(Collection<Runnable> listeners) {
        this.listeners.addAll(listeners);
    }

    public void addListener(Runnable... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }

    public void join(Consumer<AsyncFuture> future) {
        waitingFutures.add(future);
    }

    public boolean hasListener() {
        return !listeners.isEmpty();
    }

    public boolean isDone() {
        return isDone;
    }

    public void done() {
        if (waitingFutures.isEmpty()) {
            isDone = true;
            for (Runnable listener : listeners) {
                listener.run();
            }
            return;
        }
        waitingFutures.poll().accept(this);
    }
}
