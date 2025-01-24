package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.functions.ChainSupplier;

import java.util.function.Supplier;

public class TypeProcessorContextContainer<WP extends SourcedWrappedPacket<WP>, C extends TypeProcessorContext> extends TypeProcessor<WP> {

    private final Array<C> contexts = new Array<>();
    private final Supplier<C> contextFactory;

    public TypeProcessorContextContainer(Supplier<C> contextFactory) {
        this.contextFactory = contextFactory;
    }

    public synchronized C getContext(HoneyPlayer player) {
        return contexts.get(player.registryID,
                ChainSupplier.wrap(contextFactory)
                        .thenDo(() -> player.leaveCallback.addAction(Callback.Action.from(p -> contexts.remove(player.registryID)))));
    }
}
