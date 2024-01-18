package me.alek.honey.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadSeperatedQueue<T> extends ConcurrentLinkedQueue<T> {

    private final Map<Thread, ConcurrentLinkedQueue<T>> seperatedQueues = new HashMap<>();

    private ConcurrentLinkedQueue<T> getQueue() {
        return getQueue(Thread.currentThread());
    }

    private synchronized ConcurrentLinkedQueue<T> getQueue(Thread thread) {
        return seperatedQueues.computeIfAbsent(thread, (d) -> new ConcurrentLinkedQueue<>());
    }

    @Override
    public boolean add(Object object) {
        return getQueue().add((T) object);
    }

    @Override
    public T poll() {
        return poll(Thread.currentThread());
    }

    public T poll(Thread thread) {
        if (thread == null) return null;

        return getQueue(thread).poll();
    }

    @Override
    public synchronized void clear() {
        getQueue().clear();
    }

    public void clearAll() {
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
        return getQueue().size();
    }

    public int getTotalSize() {
        return seperatedQueues.values().stream().mapToInt(ConcurrentLinkedQueue::size).sum();
    }
}
