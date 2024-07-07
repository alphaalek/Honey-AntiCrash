package me.alek.honey.tickloop.packet;

import me.alek.honey.util.protocol.NetProtocol;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketTaskFactory implements PacketTaskProvider.Factory<Runnable> {


    public PacketTaskFactory() {
        // empty constructor
    }

    @Override
    public Runnable create(AtomicReference<Runnable> task,
                                     AtomicReference<Supplier<Boolean>> check,
                                     AtomicReference<Runnable> pre,
                                     AtomicReference<Runnable> post,
                                     AtomicReference<Consumer<Throwable>> catcher) {
        PacketTaskAdapter packetTask = new PacketTaskAdapter(task, check, pre, post, catcher);
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            return packetTask;
        } else {
            return new PacketTaskAdapter.PacketTaskWrapper1_13_2(packetTask);
        }
    }
}
