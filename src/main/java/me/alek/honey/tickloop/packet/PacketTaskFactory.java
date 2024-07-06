package me.alek.honey.tickloop.packet;

import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketTaskFactory implements WrappedPacketTask.Factory<FutureTask<Object>> {

    public PacketTaskFactory() {
        // empty constructor
    }

    @Override
    public FutureTask<Object> create(Runnable task, Supplier<Boolean> check, Runnable pre, Runnable post, Consumer<Throwable> catcher) {
        return new PacketTask(task, check, pre, post, catcher);
    }
}
