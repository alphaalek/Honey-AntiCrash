package me.alek.honey.validation.print;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class SuppressedException {

    private int amount;

    public void suppress(PacketContainer<? extends WrappedPacket<?>> packet, Throwable thrown) {
        amount++;

        System.out.print("Exception in thread \"" + Thread.currentThread().getName() + "\"" + (thrown.getMessage() != null ? ": " + thrown.getMessage() : ""));
        System.out.print("\tSuppressed exception (" + amount + " time(s)) while handling packet " + packet.getType() + ", ID " + packet.getType().getMinecraftID() + " by " + packet.getPlayerName());
    }
}
