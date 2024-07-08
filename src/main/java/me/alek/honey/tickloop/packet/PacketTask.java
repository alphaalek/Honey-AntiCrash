package me.alek.honey.tickloop.packet;

import com.google.common.util.concurrent.Callables;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PacketTask implements Runnable {

    public static class TaskWrapper1_13_2<T> extends FutureTask<T> {

        private final PacketTask task;

        public TaskWrapper1_13_2(PacketTask task) {
            super(Callables.returning(null));
            this.task = task;
        }

        @Override
        public void run() {
            this.task.run();
        }

        @Override
        public T get() {
            return null;
        }
    }

    private final AtomicReference<Runnable> task;
    private final AtomicReference<BooleanSupplier> check;
    private final AtomicReference<Runnable> pre;
    private final AtomicReference<Runnable> post;
    private final AtomicReference<Consumer<Throwable>> catcher;

    public PacketTask(AtomicReference<Runnable> task,
                      AtomicReference<BooleanSupplier> check,
                      AtomicReference<Runnable> pre,
                      AtomicReference<Runnable> post,
                      AtomicReference<Consumer<Throwable>> catcher) {
        this.task = task;
        this.check = check;
        this.pre = pre;
        this.post = post;
        this.catcher = catcher;
    }

    @Override
    public void run() {
        if (!check.get().getAsBoolean()) {
            return;
        }

        this.pre.get().run();
        try {
            this.task.get().run();
        } catch (Exception ex) {
            this.catcher.get().accept(ex);
        }
        this.post.get().run();
    }
}
