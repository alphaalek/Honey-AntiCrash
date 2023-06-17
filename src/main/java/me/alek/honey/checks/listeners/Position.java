package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
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

    public static class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Map<?, ?> lastChunkMap = container.getMap(0);
            if (lastChunkMap == null) {
                return ResponseBuilder.SUCCESS.builder().skip(1).build();
            }
            Chunk chunk = packet.getWorld().getChunkAt(packet.getLocation());
            if (lastChunkMap.get(player.getUniqueId()) == chunk) {
                return ResponseBuilder.SUCCESS.builder().skip(1).build();
            }
            container.putChunk(chunk);
            try {
                ((Map<UUID, Chunk>)lastChunkMap).put(player.getUniqueId(), chunk);
            } catch (Exception ex) {
                return ResponseBuilder.SUCCESS.builder().skip(1).build();
            }
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.NewChunks.get();
        }
    }

    public static class PositionB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            Chunk chunk = container.getChunk(0);
            if (chunk == null) {
                return ResponseBuilder.SUCCESS;
            }
            if (player.getWorld().isChunkLoaded(chunk) && chunk.isLoaded()) {
                return ResponseBuilder.SUCCESS;
            }
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.NewUnloadedChunks.get();
        }
    }

    public static class PositionC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (packet.getLocation().getChunk() == null) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            final int distance = Locations.calculate2DDistance(packet.getLocation(), player.getLocation());
            if (distance > ConfigurationOptions.Velocity.Movement.MAX_2D_DISTANCE) {
                player.teleport(player.getLocation());
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .flag()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.Velocity.Movement.get();
        }
    }

}
