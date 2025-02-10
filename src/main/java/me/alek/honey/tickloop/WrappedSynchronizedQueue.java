package me.alek.honey.tickloop;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class WrappedSynchronizedQueue<T> implements Queue<T> {

    protected final Queue<T> delegate;

    public WrappedSynchronizedQueue(Queue<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized int size() {
        return this.delegate.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return this.delegate.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return this.delegate.toArray();
    }

    @Override
    public synchronized <F> F[] toArray(F[] a) {
        return this.delegate.toArray(a);
    }

    @Override
    public synchronized boolean add(T o) {
        return this.delegate.add(o);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return this.delegate.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        return this.delegate.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return this.delegate.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return this.delegate.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        this.delegate.clear();
    }

    @Override
    public synchronized boolean offer(T o) {
        return this.delegate.offer(o);
    }

    @Override
    public synchronized T remove() {
        return this.delegate.remove();
    }

    @Override
    public synchronized T poll() {
        return this.delegate.poll();
    }

    @Override
    public synchronized T element() {
        return this.delegate.element();
    }

    @Override
    public synchronized T peek() {
        return this.delegate.peek();
    }
}
