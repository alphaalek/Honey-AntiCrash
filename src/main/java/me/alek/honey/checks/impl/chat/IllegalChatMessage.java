package me.alek.honey.checks.impl.chat;

import me.alek.honey.checks.Check;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.wrappers.play.client.WrappedPlayInChat;

public class IllegalChatMessage extends AsyncPacketAdapter<WrappedPlayInChat> implements Check {

    @Override
    public String getName() {
        return "Chat";
    }
}
