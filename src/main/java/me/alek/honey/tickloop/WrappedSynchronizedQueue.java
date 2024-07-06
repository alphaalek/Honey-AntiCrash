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
        return delegate.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public synchronized <F> F[] toArray(F[] a) {
        return delegate.toArray(a);
    }

    @Override
    public synchronized boolean add(T o) {
        return delegate.add(o);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized boolean offer(T o) {
        return delegate.offer(o);
    }

    @Override
    public synchronized T remove() {
        return delegate.remove();
    }

    @Override
    public synchronized T poll() {
        return delegate.poll();
    }

    @Override
    public synchronized T element() {
        return delegate.element();
    }

    @Override
    public synchronized T peek() {
        return delegate.peek();
    }
}
