package me.alek.honey.utility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadSeperatedQueue<T> extends ConcurrentLinkedQueue<T> {

    private final Map<Thread, ConcurrentLinkedQueue<T>> seperatedQueues = new HashMap<>();

    @Override
    public boolean add(Object object) {
        return seperatedQueues.computeIfAbsent(Thread.currentThread(), (d) -> new ConcurrentLinkedQueue<>())
                .add((T) object);
    }

    @Override
    public T poll() {
        return poll(Thread.currentThread());
    }

    public T poll(Thread thread) {
        if (thread == null) return null;

        return seperatedQueues.computeIfAbsent(thread, (d) -> new ConcurrentLinkedQueue<>())
                .poll();
    }

    @Override
    public void clear() {
        seperatedQueues.clear();
    }

    public Thread getAnyQueuedThread() {
        for (Thread thread : seperatedQueues.keySet()) {
            if (!seperatedQueues.get(thread).isEmpty())
                return thread;
        }
        return null;
    }

    public int getSize() {
        return seperatedQueues.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
    }
}
