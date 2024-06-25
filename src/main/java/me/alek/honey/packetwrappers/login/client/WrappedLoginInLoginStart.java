package me.alek.honey.packetwrappers.login.client;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;

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
