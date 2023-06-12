package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUpdateSign;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class IllegalSign extends AsyncPacketAdapter<WrappedPlayInUpdateSign> implements Check {

    private final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        for (String line : packetContainer.getPacket().getLines()) {
            if (line.length() > 49) {
                packetContainer.cancel();
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal Component");
            }
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.UPDATE_SIGN);
    }

    @Override
    public String getName() {
        return "Sign Update";
    }
}
