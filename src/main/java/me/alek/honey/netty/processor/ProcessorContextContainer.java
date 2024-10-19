package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.chain.ChainSupplier;

import java.util.function.Supplier;

public class ProcessorContextContainer<WP extends WrappedPacket<WP>, C extends ProcessorContext> extends Processor<WP> {

    private final Array<C> contexts = new Array<>();
    private final Supplier<C> contextFactory;

    public ProcessorContextContainer(Supplier<C> contextFactory) {
        this.contextFactory = contextFactory;
    }

    public synchronized C getContext(HoneyPlayer player) {
        return contexts.get(player.registryID,
                ChainSupplier.wrap(contextFactory)
                        .thenDo(() -> player.leaveCallback.addAction(Callback.Action.from(p -> contexts.remove(player.registryID)))));
    }
}
