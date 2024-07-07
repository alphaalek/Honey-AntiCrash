package me.alek.honey.tickloop.packet;

import com.google.common.util.concurrent.Callables;
import me.alek.honey.util.chain.ChainSupplier;
import org.bukkit.Bukkit;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketTaskAdapter implements Runnable {

    public static class PacketTaskWrapper1_13_2 extends FutureTask<Object> {

        private final PacketTaskAdapter task;

        public PacketTaskWrapper1_13_2(PacketTaskAdapter task) {
            super(Callables.returning(null));
            this.task = task;

            task.check.set(ChainSupplier.wrap(task.check.get()).thenDoIf(p -> !p, () -> super.set(null)));
        }

        @Override
        public void run() {
            this.task.run();
        }
    }

    private final AtomicReference<Runnable> task;
    private final AtomicReference<Supplier<Boolean>> check;
    private final AtomicReference<Runnable> pre;
    private final AtomicReference<Runnable> post;
    private final AtomicReference<Consumer<Throwable>> catcher;

    public PacketTaskAdapter(AtomicReference<Runnable> task,
                             AtomicReference<Supplier<Boolean>> check,
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
        Bukkit.getLogger().info("Running");

        if (!check.get().get()) {
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
