package me.alek.honey.tickloop;

import me.alek.honey.tickloop.postpone.PostponeHandler;

public class TickloopHandler {

    private final PostponeHandler postponeHandler = new PostponeHandler();

    public PostponeHandler getPostponeHandler() {
        return postponeHandler;
    }
}
