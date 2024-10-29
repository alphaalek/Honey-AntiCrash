package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.ProcessorContext;
import me.alek.honey.netty.processor.ProcessorContextContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInClientCommand;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCloseWindow;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutOpenWindow;
import me.alek.honey.util.protocol.NetProtocol;

public class InventoryProcessor<WP extends WrappedPacket<WP>> extends ProcessorContextContainer<WP, InventoryProcessor.InventoryEmulator> {

    public static final int CRAFTING_SLOTS = 5;
    public static final int ARMOR_SLOTS = 5;
    public static final int ROW_SLOTS = 9;
    public static final int INVENTORY_SLOTS = 4 * ROW_SLOTS;

    public InventoryProcessor() {
        super(InventoryEmulator::new);
        addListener(PacketTypes.Play.Client.CLOSE_WINDOW, PacketContainer.PipelineState.OBJECT, this::handleCloseWindow);
        addListener(PacketTypes.Play.Client.CLIENT_COMMAND, PacketContainer.PipelineState.OBJECT, this::handleClientCommand);
        addListener(PacketTypes.Play.Client.CLICK_WINDOW, PacketContainer.PipelineState.OBJECT, this::handleClickWindow);
        addListener(PacketTypes.Play.Server.OPEN_WINDOW, PacketContainer.PipelineState.OBJECT, this::handleOpenWindow);
    }

    private boolean sendsPacketForOpenInventory(HoneyPlayer player) {
        return NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_15_2)
                && player.clientInfo.getProtocol().isOlderThanOrEqual(NetProtocol.v1_15_2);
    }

    public void handleCloseWindow(HoneyPlayer player, PacketContainer<WrappedPlayInCloseWindow> packet) {
        getContext(player).container = null;
    }

    public void handleClientCommand(HoneyPlayer player, PacketContainer<WrappedPlayInClientCommand> packet) {
        if (sendsPacketForOpenInventory(player)) {
            getContext(player).container = WrappedPlayOutOpenWindow.Container.PLAYER;
        }
    }

    public void handleClickWindow(HoneyPlayer player, PacketContainer<WrappedPlayInWindowClick> packet) {
        if (!sendsPacketForOpenInventory(player)) {
            getContext(player).container = WrappedPlayOutOpenWindow.Container.PLAYER;
        }
    }

    public void handleOpenWindow(HoneyPlayer player, PacketContainer<WrappedPlayOutOpenWindow> packet) {
        getContext(player).container = packet.getAnyWrapped().getInventoryContainer();
    }

    public static class InventoryEmulator implements ProcessorContext {

        public WrappedPlayOutOpenWindow.Container container;

    }
}
