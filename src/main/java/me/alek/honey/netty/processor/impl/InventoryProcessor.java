package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.Emulator;
import me.alek.honey.netty.processor.ProcessorEmulator;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCloseWindow;

public class InventoryProcessor<WP extends WrappedPacket<WP>> extends ProcessorEmulator<WP, InventoryProcessor.InventoryEmulator> {

    public InventoryProcessor() {
        super(InventoryEmulator::new);
        addListener(PacketTypes.Play.Client.CLOSE_WINDOW, this::handleCloseWindow);

    }

    public void handleCloseWindow(HoneyPlayer player, PacketContainer<WrappedPlayInCloseWindow> packet) {
        getContext(player).windowOpen = false;
    }

    public static class InventoryEmulator implements Emulator {

        public boolean windowOpen;

    }
}
