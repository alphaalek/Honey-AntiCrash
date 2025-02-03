package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.reflection.MinecraftReflection;

public class WrappedPlayInClientCommand extends ObjectWrappedPacket<WrappedPlayInClientCommand> {

    public WrappedPlayInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Action getAction() {
        Enum<?> val = (Enum<?>) getModifier(MinecraftReflection.getEnumClientCommandActionClass()).readField(0);
        return Action.values()[val.ordinal()];
    }

    public enum Action {
        PERFORM_RESPAWN, REQUEST_STATS, OPEN_INVENTORY_ACHIEVEMENT;
    }
}
