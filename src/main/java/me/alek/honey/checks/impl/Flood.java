package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Supplier;

public class Flood extends FuzzyPacketAdapter implements Check {

    private static final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer packetContainer) {
        //VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this);
    }

    @Override
    public void onPacketCancel(Player player, PacketContainer packetContainer, PacketBound bound) {
        // something here
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol());
    }

    @Override
    public String getName() {
        return "Packets";
    }
}
