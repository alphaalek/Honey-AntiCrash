package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCloseWindow;

public class InventoryProcessor<WP extends WrappedPacket<WP>> extends Processor<WP, InventoryProcessor.Emulator> {

    public InventoryProcessor() {
        super(Emulator::new);
        add(PacketTypes.Play.Client.CLOSE_WINDOW, this::handleCloseWindow);

    }

    public void handleCloseWindow(HoneyPlayer player, PacketContainer<WrappedPlayInCloseWindow> packet) {
        getContext(player).windowOpen = false;
    }

    public static class Emulator implements ProcessorContext {

        public boolean windowOpen;

    }
}
