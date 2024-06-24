package me.alek.honey.netty.packet;

public interface Cancellable {

    void cancel();

    boolean isCancelled();

}
