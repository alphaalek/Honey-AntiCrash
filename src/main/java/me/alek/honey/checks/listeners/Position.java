package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.PositionA;
import me.alek.honey.checks.impl.PositionB;
import me.alek.honey.checks.impl.PositionC;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.utility.Locations;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Position<WP extends WrappedPlayInFlying<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private final Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        if (Locations.compare3DLocation(packetContainer.getPacket().getLocation(), player.getLocation())) {
            return false;
        }
        container.putMap(lastChunkMap);
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new PositionA<>());
        visitor.visit(new PositionB<>());
        visitor.visit(new PositionC<>());
        return visitor;
    }

    @Override
    public void cleanup(Player player) {
        lastChunkMap.remove(player.getUniqueId());
    }

}
