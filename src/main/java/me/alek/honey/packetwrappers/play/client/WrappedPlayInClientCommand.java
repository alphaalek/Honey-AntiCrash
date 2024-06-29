package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.reflection.NMS;

public class WrappedPlayInClientCommand extends WrappedPacket<WrappedPlayInClientCommand> {

    public WrappedPlayInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Action getAction() {
        Enum<?> val = (Enum<?>) getModifier(NMS.getEnumClientCommandActionClass()).readField(0);
        return Action.values()[val.ordinal()];
    }

    public enum Action {
        PERFORM_RESPAWN, REQUEST_STATS, OPEN_INVENTORY_ACHIEVEMENT;
    }
}
