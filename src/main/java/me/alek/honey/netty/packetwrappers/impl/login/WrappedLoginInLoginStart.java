package me.alek.honey.netty.packetwrappers.impl.login;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.modelwrappers.objects.WrappedGameProfile;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;

public class WrappedLoginInLoginStart extends ObjectWrappedPacket<WrappedLoginInLoginStart> {

    public WrappedLoginInLoginStart(Object rawPacket, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public WrappedGameProfile getProfile() {
        return getContainer().getStructure().getGameProfiles().readField(0);
    }

    public String getName() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_18_2)) {
            return getProfile().getName();
        } else {
            return getContainer().getStructure().getStrings().readField(0);
        }
    }
}
