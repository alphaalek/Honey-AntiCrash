package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import me.alek.honey.netty.processor.TypeProcessor;

public class LoginProcessor extends TypeProcessor<WrappedLoginInLoginStart> {

    public static void load() {
        LoginProcessor processor = new LoginProcessor();
        processor.addListener(PacketTypes.Login.Client.HELLO, PacketContainer.PipelineState.INITIAL_BYTE, processor::handleLogin);
    }

    public void handleLogin(HoneyPlayer player, PacketContainer<WrappedLoginInLoginStart> packet) {
        packet.getPlayer().setupHello(packet.getCurrentWrapped());

        HoneyPacketHandler.AWAITING_PROFILES.put(packet.getPlayerName(), packet.getPacketHandler());
    }
}
