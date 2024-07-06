package me.alek.honey.netty.packetwrappers.login.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginInLoginStart extends WrappedPacket<WrappedLoginInLoginStart> {

    public WrappedLoginInLoginStart(Object rawPacket, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public WrappedGameProfile getProfile() {
        return getContainer().getGameProfiles().readField(0);
    }

    public String getName() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_18_2)) {
            return getContainer().getGameProfiles().readField(0).getName();
        } else {
            return getContainer().getStrings().readField(0);
        }
    }
}
