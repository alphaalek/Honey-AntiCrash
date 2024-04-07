package me.alek.honey.tickloop;

import me.alek.honey.tickloop.postpone.PostponeHandler;
import me.alek.honey.tickloop.postpone.checkimpl.PostponeIgnoreTerminator;
import me.alek.honey.tickloop.postpone.checkimpl.PostponeRateCheck;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tracker.packet.WrappedPacketTaskFuture;

import java.util.LinkedList;
import java.util.Queue;

public class TickloopHandler {

    private final PostponeHandler postponeHandler = new PostponeHandler();

    public PostponeHandler getPostponeHandler() {
        return postponeHandler;
    }
}
