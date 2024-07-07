package me.alek.honey.tickloop.packet;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PacketTaskFactory implements PacketTaskProvider.Factory<Runnable> {

    private final boolean modern;

    public PacketTaskFactory(boolean modern) {
        this.modern = modern;
    }

    @Override
    public Runnable create(AtomicReference<Runnable> task,
                           AtomicReference<BooleanSupplier> check,
                           AtomicReference<Runnable> pre,
                           AtomicReference<Runnable> post,
                           AtomicReference<Consumer<Throwable>> catcher) {
        PacketTaskAdapter packetTask = new PacketTaskAdapter(task, check, pre, post, catcher);
        if (modern) {
            return packetTask;
        } else {
            return new PacketTaskAdapter.TaskWrapper1_13_2<>(packetTask);
        }
    }
}
