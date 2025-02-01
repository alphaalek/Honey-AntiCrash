package me.alek.honey.netty.packet;

import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

public abstract class PacketContainerLifecycle<WP extends SourcedWrappedPacket<WP>> implements PacketContainer<WP> {

    protected boolean cancelled;
    protected boolean handled;
    protected boolean logged;
    protected boolean dumped;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public boolean isHandled() {
        return this.handled;
    }

    @Override
    public boolean isDumped() {
        return this.dumped;
    }

    @Override
    public boolean isLogged() {
        return this.logged;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public void handle() {
        this.handled = true;
    }

    @Override
    public void dump() {
        this.dumped = true;
    }

    @Override
    public void log() {
        this.logged = true;
    }
}
