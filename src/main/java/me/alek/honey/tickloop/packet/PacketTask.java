package me.alek.honey.tickloop.packet;

import org.bukkit.Bukkit;

import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketTask extends FutureTask<Object> {

    private final Supplier<Boolean> check;
    private final Runnable pre;
    private final Runnable post;
    private final Consumer<Throwable> catcher;

    public PacketTask(Runnable runnable, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        super(runnable, null);
        this.check = check;
        this.pre = pre;
        this.post = post;
        this.catcher = catcher;
    }

    @Override
    public void run() {
        if (!check.get()) {
            super.set(null);
            return;
        }

        this.pre.run();
        try {
            super.run();
        } catch (Exception ex) {
            this.catcher.accept(ex);
        }
        this.post.run();
    }
}
