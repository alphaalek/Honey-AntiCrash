package me.alek.honey.netty.packetwrappers.login.client;

import com.mojang.authlib.GameProfile;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedLoginInLoginStart extends WrappedPacket<WrappedLoginInLoginStart> {

    public WrappedLoginInLoginStart(Object rawPacket, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public GameProfile getProfile() {
        return getContainer().getObjects(GameProfile.class).read(0);
    }
}
