package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.ChainSupplier;

import java.util.function.Supplier;

public class ProcessorEmulator<WP extends WrappedPacket<WP>, C extends Emulator> extends Processor<WP> {

    private final Array<C> contexts = new Array<>();
    private final Supplier<C> contextFactory;

    public ProcessorEmulator(Supplier<C> contextFactory) {
        this.contextFactory = contextFactory;
    }

    public synchronized C getContext(HoneyPlayer player) {
        return contexts.get(player.registryID,
                ChainSupplier.wrap(contextFactory)
                        .thenDo(() -> player.leaveCallback.addAction(p -> contexts.remove(player.registryID))));
    }
}
