package me.alek.honey.netty.api.event;

public interface EventExecutor {

    void call(Event event);
}
